package br.com.server.model.impl;

import br.com.server.model.AbstractClientTracker;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 *
 * @author Guilherme Alves
 */
public class SizeClientTracker extends AbstractClientTracker<Integer> {

    private static final int WITHOUT_REQUEST_BODY_LENGHT = -1;
    private int requestBodyLength = WITHOUT_REQUEST_BODY_LENGHT;

    public SizeClientTracker(long timeout, SocketChannel client) {
        super(timeout, client);
    }

    public SizeClientTracker(long timeout, boolean updateTime, SocketChannel client) {
        super(timeout, updateTime, client);
    }

    /**
     * Analisa se a requisição atingiu o tamanho informado.
     * @param requestHeaderLength
     * @return 
     */
    @Override
    public boolean isEndOfRequest(Integer requestHeaderLength) {
        if (isEmpty() || getTotalBytesLength() < requestHeaderLength) {
            return false;
        }

        if (WITHOUT_REQUEST_BODY_LENGHT == requestBodyLength) {
            ByteBuffer bodySize = mountPartRequest(requestHeaderLength);
            if (bodySize.remaining() < requestHeaderLength) {
                return false;
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < requestHeaderLength; i++) {
                sb.append((char) bodySize.get());
            }

            this.requestBodyLength = Integer.parseInt(sb.toString());
        }
        
        return (getTotalBytesLength() - requestHeaderLength) >= this.requestBodyLength;
    }

    @Override
    public boolean isTimeoutReached() {
        return false; //Temporário
    }
}
