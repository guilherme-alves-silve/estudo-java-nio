package br.com.server;

import static br.com.MainServer.DEBUG;

import br.com.server.model.AbstractClientTracker;
import br.com.server.model.StatusClientTracker;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Classe de examplo usando o selector em um socket
 *
 * @author Guilherme Alves Silveira
 */
public abstract class AbstractServerController<T extends AbstractClientTracker>
        extends AbstractServerNIO {

    private static final int CLIENT_DESCONNECTED = -1;
    private static final int INVISIBLE_TO_SELECT = 0;
    protected final ConcurrentMap<SocketChannel, T> tracker = new ConcurrentHashMap<>();
    protected final long timeout;
    protected final int buffSize;

    public AbstractServerController(
            long timeout,
            int buffSize,
            ServerSocketChannel server,
            Selector selector
    ) throws ClosedChannelException {
        super(server, selector);
        this.timeout = timeout;
        this.buffSize = buffSize;
    }

    @Override
    protected void before() {
        writeToClientsWithTimeout();
    }

    @Override
    protected void between() {
        //Do nothing
    }

    @Override
    protected void beginLoop() {
        //Do nothing
    }

    @Override
    protected void after() {
        //Do nothing
    }

    /**
     * Utilizado para ser sobreescrito para processar algo quando um timeout
     * ocorre.
     */
    protected abstract void whenTimeout();

    private void writeToClientsWithTimeout() {
        synchronized (this) {
            tracker.forEach((socketClient, clientTracker) -> {
                try {
                    if (clientTracker.getStatus() == StatusClientTracker.PROCESS) {
                        return;
                    }

                    if (timeoutSocketClient(clientTracker)) {
                        whenTimeout();
                        invisibleToSelect(socketClient);
                        process(clientTracker).whenComplete((nothing, th) -> {
                            try {
                                registerInSelector(socketClient, SelectionKey.OP_WRITE);
                            } catch (IOException ex) {
                                ex.printStackTrace();
                                invalidateClient(socketClient);
                            }
                        });
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                    invalidateClient(socketClient);
                    try {
                        socketClient.close();
                    } catch (IOException ex1) {
                        ex1.printStackTrace();
                    }
                }
            });
        }
    }

    private void invisibleToSelect(SocketChannel socketClient) throws ClosedChannelException {
        socketClient.register(selector, INVISIBLE_TO_SELECT);
    }

    /**
     * Metodo que trata da conexao do cliente. Esse recbe como arqumento uma
     * instancia do ServerScocketCliente e um Selector. Logo apos será
     * configurado alguns parametros referentes a conexao com o cliente, e
     * configuaração do modo como ele irar tratar a conexao, se será bloqueante
     * ou nao.Tambem a configuração do metodo registro que irar receber uma
     * instancia do Selector e a flag indicando que o socket ira ler do cliente
     *
     * @param server
     * @param selector
     * @throws ClosedChannelException
     * @throws IOException
     */
    @Override
    protected void acceptClient()
            throws ClosedChannelException, IOException {
        //Pode ser feito desta maneira também:
        //ServerSocketChannel s = (ServerSocketChannel) key.channel();
        //SocketChannel socket = s.accept();
        //Ou dessa:
        SocketChannel socketClient = server.accept();//aceitando o cliente
        createClientTracker(socketClient);
        socketClient.configureBlocking(false);//configura para nao bloqueante
        socketClient.register(selector, SelectionKey.OP_READ);//registra para ler no proximo envio do cliente
    }

    private void createClientTracker(SocketChannel socketClient) {
        tracker.put(socketClient, newClientTracker(socketClient));
    }

    /**
     * Esse metodo é resonsavel por ler informações na forma de byte do
     * cliente,ou seja ele irá receber e ler os dados enviados do cliente.
     *
     * @param key
     * @param selector
     * @throws ClosedChannelException
     * @throws IOException
     */
    @Override
    protected void readFromClient(SelectionKey key)
            throws ClosedChannelException, IOException {

        //instancia um objeto do tipo ByteBuffer e limita para receber 1024 bytes
        ByteBuffer buf = ByteBuffer.allocateDirect(buffSize);
        //recebe a instancia key de acordo com a condicional definida na chamada. Aqui o retorno e um objeto generico
        //do tipo SelectableChannel ou seja um channel do tipo selecionavel
        SocketChannel socketClient = (SocketChannel) key.channel();
        T clientTracker = tracker.get(socketClient);
        if (!tryRead(socketClient, buf)) {
            invalidateClient(socketClient);
            return;
        }

        clientTracker.addRequestPart(buf);
        if (!isEndOfMessage(clientTracker)
                && !timeoutSocketClient(clientTracker)) {
            socketClient.register(selector, SelectionKey.OP_READ);
            return;
        }

        if (clientTracker.getStatus() == StatusClientTracker.TIMEOUT) {
            return;
        }

        if (DEBUG) {
            System.out.println("PROCESSED " + clientTracker.getClient());
        }

        clientTracker.setStatus(StatusClientTracker.PROCESS);
        invisibleToSelect(socketClient);
        process(clientTracker).whenComplete((nothing, th) -> {
            if (!socketClient.isOpen()) {
                return;
            }
            
            try {
                System.out.println("COMPLETED!");
                registerInSelector(socketClient, SelectionKey.OP_WRITE);
            } catch (ClosedChannelException ex) {
                invalidateClient(socketClient);
            }
        });
    }

    private boolean tryRead(SocketChannel socketClient, ByteBuffer buf) {
        int readed;
        try {
            readed = socketClient.read(buf);
            buf.flip();
        } catch (IOException ex) {
            ex.printStackTrace();
            return false;
        }

        return CLIENT_DESCONNECTED != readed;
    }

    protected abstract T newClientTracker(SocketChannel socketClient);

    protected abstract boolean isEndOfMessage(T clientTracker);

    @Override
    protected final void writeToClient(SelectionKey key)
            throws IOException {
        SocketChannel socketClient = (SocketChannel) key.channel();
        T clientTracker = tracker.get(socketClient);
        try {
            if (null == clientTracker || null == clientTracker.getResponse()) {
                System.out.println("AQUI");
            }

            int writed = socketClient.write(clientTracker.getResponse());
            if (timeoutSocketClient(clientTracker)) {
                tracker.remove(socketClient);
            }

            if (CLIENT_DESCONNECTED == writed
                    || isClientDisconnected(socketClient)) {
                if (DEBUG) {
                    System.out.println(String.format("[%s DISCONNECTED]",
                            socketClient.getRemoteAddress()));
                }
                invalidateClient(socketClient);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            invalidateClient(socketClient);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private boolean isClientDisconnected(SocketChannel socketClient)
            throws IOException {
        ByteBuffer clientDesconnected = ByteBuffer.allocate(1);
        return CLIENT_DESCONNECTED == socketClient.read(clientDesconnected);
    }

    /**
     * Registra o SocketChannel com a operação informada e notifica o selector
     * que existe channels para serem processados. Caso o selector não seja
     * notificado (se esse método não for chamado), o mesmo, só ira processar as
     * chaves na proxima iteração (podendo ser nunca, dependendo de como está
     * configurado o tempo de espera para ser executado cada iteração no
     * {@code selector.select(TEMPO_AQUI_EM_MS)}), se notificado, a iteração já
     * é processada no mesmo momento.
     *
     * @param socketClient Socket do cliente
     * @param operation Operação presente nas constantes do objeto SelectionKey,
     * tais como OP_READ, OP_WRITE, etc.
     * @throws ClosedChannelException
     */
    private void registerInSelector(
            SocketChannel socketClient,
            int operation
    ) throws ClosedChannelException {
        socketClient.register(selector, operation);
        selector.wakeup();
    }

    private void invalidateClient(SocketChannel socketClient) {
        try {
            tracker.remove(socketClient);
            socketClient.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected boolean timeoutSocketClient(AbstractClientTracker clientTracker)
            throws IOException {
        if (clientTracker.getStatus() == StatusClientTracker.TIMEOUT) {
            return true;
        } else if (clientTracker.getStatus() == StatusClientTracker.PROCESS) {
            return false;
        }

        boolean isTimeout = clientTracker.isTimeoutReached();
        if (isTimeout) {

            if (DEBUG) {
                System.out.println("TIMEOUT " + clientTracker.getClient());
            }

            clientTracker.setStatus(StatusClientTracker.TIMEOUT);
        }
        return isTimeout;
    }

    protected abstract <U> CompletableFuture<U> process(T clientTracker);
}
