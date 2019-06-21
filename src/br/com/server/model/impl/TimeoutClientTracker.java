package br.com.server.model.impl;

import br.com.server.model.AbstractClientTracker;
import java.nio.channels.SocketChannel;

/**
 *
 * @author Guilherme Alves Silveira.
 */
public class TimeoutClientTracker extends AbstractClientTracker {

    public TimeoutClientTracker(long timeout, SocketChannel client) {
        super(timeout, client);
    }

    public TimeoutClientTracker(long timeout, boolean updateTime, SocketChannel client) {
        super(timeout, updateTime, client);
    }
    
    /**
     * Simplemente retorna se o timeout foi atingido.
     */
    @Override
    public boolean isEndOfRequest(Object ignore) {
        return timeoutReached();
    }
}