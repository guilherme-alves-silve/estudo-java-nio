package br.com.server.impl;

import br.com.server.AbstractServerController;
import br.com.server.model.impl.TimeoutClientTracker;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.CompletableFuture;

/**
 *
 * @author Guilherme Alves Silveira
 */
public class TimeoutServerController extends AbstractServerController<TimeoutClientTracker> {

    public TimeoutServerController(
            long timeout,
            int buffSize, 
            ServerSocketChannel server, 
            Selector selector
    ) throws ClosedChannelException {
        super(timeout, buffSize, server, selector);
    }

    @Override
    protected TimeoutClientTracker newClientTracker(SocketChannel socketClient) {
        return new TimeoutClientTracker(timeout, socketClient);
    }

    @Override
    protected boolean isEndOfMessage(TimeoutClientTracker clientTracker) {
        return clientTracker.isEndOfRequest(null);
    }

    @Override
    protected CompletableFuture<Void> process(TimeoutClientTracker clientTracker) {
        return CompletableFuture.runAsync(() -> {
            byte[] request = clientTracker.mountByteRequest();
            String input = new String(request);
            String response = "SUCESSO! PROCESSADO " + input.length() + " BYTES!";
            clientTracker.setResponse(response.getBytes());
        });
    }

    @Override
    protected void whenTimeout() {
        System.out.println("TimeoutServerController - TIMEOUT ");
    }
}
