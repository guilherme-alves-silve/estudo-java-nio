package br.com.server.model.impl;

import br.com.server.model.AbstractClientTracker;
import java.nio.channels.SocketChannel;

/**
 *
 * @author Guilherme Alves
 */
public class FixedSizeClientTracker extends AbstractClientTracker<Integer> {

    public FixedSizeClientTracker(long timeout, SocketChannel client) {
        super(timeout, client);
    }

    public FixedSizeClientTracker(long timeout, boolean updateTime, SocketChannel client) {
        super(timeout, updateTime, client);
    }

    /**
     * Analisa se a requisição atingiu o tamanho informado.
     * @param requestHeaderLength
     * @return 
     */
    @Override
    public boolean isEndOfRequest(Integer fixedSize) {
        return getTotalBytesLength() >= fixedSize;
    }
}
