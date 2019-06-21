package br.com.server.impl;

import br.com.server.AbstractServerController;
import br.com.server.model.impl.SizeClientTracker;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletableFuture;

/**
 *
 * @author Guilherme Alves Silveira
 */
public class SizeServerController extends AbstractServerController<SizeClientTracker> {

    private static final int HEADER_BODY_LENGTH = 5;

    public SizeServerController(
            long timeout, 
            int buffSize,
            ServerSocketChannel server, 
            Selector selector
    ) throws ClosedChannelException {
        super(timeout, buffSize, server, selector);
    }

    @Override
    protected SizeClientTracker newClientTracker(SocketChannel socketClient) {
        return new SizeClientTracker(timeout, socketClient);
    }

    @Override
    protected boolean isEndOfMessage(SizeClientTracker clientTracker) {
        return clientTracker.isEndOfRequest(HEADER_BODY_LENGTH);
    }

    @Override
    protected CompletableFuture<Void> process(SizeClientTracker clientTracker) {
        return CompletableFuture.runAsync(() -> {
            byte[] request = clientTracker.mountByteRequest();
            String input = new String(request);
            String response = "SUCESSO! PROCESSADO " + input.length() + " BYTES!";
            clientTracker.setResponse(response.getBytes());
        });
    }

    @Override
    protected void whenTimeout() {
        System.out.println("SizeServerController - TIMEOUT ");
    }
}
