package br.com.server.impl;

import br.com.server.AbstractServerController;
import br.com.server.model.impl.EndClientTracker;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletableFuture;

/**
 *
 * @author Guilherme Alves Silveira
 */
public class EndServerController extends AbstractServerController<EndClientTracker> {

    private static final byte[] CRLF = "\r\n".getBytes();

    public EndServerController(
            long timeout,
            int buffSize,
            ServerSocketChannel server, 
            Selector selector
    ) throws ClosedChannelException {
        super(timeout, buffSize, server, selector);
    }

    @Override
    protected EndClientTracker newClientTracker(SocketChannel socketClient) {
        return new EndClientTracker(timeout, socketClient);
    }

    @Override
    protected boolean isEndOfMessage(EndClientTracker clientTracker) {
        return clientTracker.isEndOfRequest(CRLF);
    }

    @Override
    protected CompletableFuture<Void> process(EndClientTracker clientTracker) {
        return CompletableFuture.runAsync(() -> {
            byte[] request = clientTracker.mountByteRequest();
            String input = new String(request);
            String response = "SUCESSO! PROCESSADO " + input.length() + " BYTES!";
            clientTracker.setResponse(response.getBytes());
        });
    }

    @Override
    protected void whenTimeout() {
        System.out.println("EndServerController - TIMEOUT ");
    }
}
