package br.com.server.impl;

import br.com.server.AbstractServerController;
import br.com.server.model.impl.FixedSizeClientTracker;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletableFuture;

/**
 *
 * @author Guilherme Alves Silveira
 */
public class FixedSizeServerController extends AbstractServerController<FixedSizeClientTracker> {

    private static final int FIXED_REQUEST_LENGTH = 64;
    
    public FixedSizeServerController(
            long timeout,
            int buffSize,
            ServerSocketChannel server, 
            Selector selector
    ) throws ClosedChannelException {
        super(timeout, buffSize, server, selector);
    }

    @Override
    protected FixedSizeClientTracker newClientTracker(SocketChannel socketClient) {
        return new FixedSizeClientTracker(timeout, socketClient);
    }

    @Override
    protected boolean isEndOfMessage(FixedSizeClientTracker clientTracker) {
        return clientTracker.isEndOfRequest(FIXED_REQUEST_LENGTH);
    }

    @Override
    protected CompletableFuture<Void> process(FixedSizeClientTracker clientTracker) {
        return CompletableFuture.runAsync(() -> {
            byte[] request = clientTracker.mountByteRequest();
            String input = new String(request);
            String response = "SUCESSO! PROCESSADO " + input.length() + " BYTES!";
            clientTracker.setResponse(response.getBytes());
        });
    }

    @Override
    protected void whenTimeout() {
        System.out.println("FixedSizeServerController - TIMEOUT ");
    }
}
