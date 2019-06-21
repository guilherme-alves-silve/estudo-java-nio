package br.com.server.model.impl;

import br.com.server.model.AbstractClientTracker;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 *
 * @author Guilherme Alves
 */
public class EndClientTracker extends AbstractClientTracker<byte[]> {

    public EndClientTracker(long timeout, SocketChannel client) {
        super(timeout, client);
    }

    public EndClientTracker(long timeout, boolean updateTime, SocketChannel client) {
        super(timeout, updateTime, client);
    }
    
    /**
     * Compara o byteEnd com os ultimos bytes presentes nas partes 
     * da requisição. Se o ultimo byte não conter informação o suficiente,
     * o byte anterior ao ultimo será processado junto do ultimo, até que se tenha
     * tamanho suficiente para realizar a comparação do byteEnd com os bytes juntados.
     * Caso não tenha bytes o suficientes para serem processados o método retorna false.
     * @param byteEnd
     * @return 
     */
    @Override
    public boolean isEndOfRequest(byte[] byteEnd) {
        Objects.requireNonNull(byteEnd);
        List<ByteBuffer> requests = getPartReadOnlyRequests();
        ByteBuffer last = requests.get(requests.size() - 1);
        if (last.limit() >= byteEnd.length) {
            return endOfRequest(last, byteEnd);
        }

        byte[] bufferEnd = new byte[byteEnd.length];
        int offset = 1;
        for (int i = requests.size() - 1; i >= 0; i--) {
            ByteBuffer buff = requests.get(i);
            int posBuff = buff.limit() - 1;
            for (int j = bufferEnd.length - offset; j >= 0 && posBuff >= 0; j--) {
                bufferEnd[j] = buff.get(posBuff--);
                offset++;
            }
        }

        return Arrays.equals(byteEnd, bufferEnd);
    }

    private boolean endOfRequest(ByteBuffer buf, byte[] byteEnd) {
        byte[] bufferEnd = new byte[byteEnd.length];
        ByteBuffer duplicatedBuff = buf.duplicate();
        if (!duplicatedBuff.hasRemaining()) {
            duplicatedBuff.flip();
        }
        
        int readed = buf.limit();
        if (readed < byteEnd.length) {
            return false;
        }

        duplicatedBuff.position(readed - byteEnd.length);
        duplicatedBuff.get(bufferEnd);
        return Arrays.equals(byteEnd, bufferEnd);
    }
}