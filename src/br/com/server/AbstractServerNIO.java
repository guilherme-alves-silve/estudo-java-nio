package br.com.server;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.Iterator;
import java.util.Objects;

/**
 * @author Guilherme Alves Silveira
 * @author Francisco Araujo
 */
public abstract class AbstractServerNIO {

    protected final ServerSocketChannel server;
    protected final Selector selector;

    public AbstractServerNIO(
            ServerSocketChannel server,
            Selector selector
    ) throws ClosedChannelException {
        this.server = Objects.requireNonNull(server);
        this.selector = Objects.requireNonNull(selector);

        if (!selector.isOpen()) {
            throw new IllegalArgumentException("The selector must be opened.");
        }

        if (server.isBlocking()) {
            throw new IllegalArgumentException("The server must be non-blocking.");
        }

        server.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started!");
    }

    public void runEventLoop() throws IOException {
        while (true) {

            before();
            //configurando o tempo limite de espera para um cliente em ms
            int readyChannels = selector.select(50);
            if (0 == readyChannels) {
                continue;
            }

            between();
            //Retorno uma coloecao Iterator de keys do tipo isReadable isWritable e isAcceptable
            Iterator<SelectionKey> itKeys = selector.selectedKeys().iterator();
            //faz um loop while equanto tiver key
            while (itKeys.hasNext()) {
                beginLoop();
                SelectionKey key = itKeys.next();//vai para a proxima key
                itKeys.remove();//remove a key anterior
                try {
                    if (key.isAcceptable()) {
                        acceptClient();
                    } else if (key.isReadable()) {
                        readFromClient(key);
                    } else if (key.isWritable()) {
                        writeToClient(key);
                    }
                } catch (Throwable th) {
                    th.printStackTrace();
                }
            }

            after();
        }
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
    protected abstract void acceptClient() throws Exception;

    /**
     * Esse metodo é responsavel por ler informações na forma de byte do
     * cliente,ou seja ele irá receber e ler os dados enviados do cliente.
     *
     * @param key
     * @param selector
     * @throws ClosedChannelException
     * @throws IOException
     */
    protected abstract void readFromClient(SelectionKey key) throws Exception;

    /**
     * Esse metodo é responsavel por enviar os dados para o cliente quando já
     * estão processados.
     *
     * @param key
     * @param selector
     * @throws ClosedChannelException
     * @throws IOException
     */
    protected abstract void writeToClient(SelectionKey key) throws Exception;

    /**
     * Executado antes do channel realizar qualquer procedimento. Será chamado
     * uma vez a cada vez que o eventLoop é rodado.
     */
    protected abstract void before();

    /**
     * Executado depois do before e depois que o selector encontra algum channel
     * para processar. Será chamado uma vez a cada vez que o eventLoop é rodado.
     */
    protected abstract void between();

    /**
     * É executado dentro do eventLoop e enquanto tiver chaves (as chaves são as
     * operações que o cliente vai realizar, tais como ser aceito pelo servidor,
     * escrever para o servidor ou ler do servidor) para serem processadas. Será
     * chamado 1-1 para cada chave antes de ser processada.
     */
    protected abstract void beginLoop();

    /**
     * Executado no fim do eventLoop. Será chamado uma vez a cada vez que o
     * eventLoop é rodado.
     */
    protected abstract void after();
}
