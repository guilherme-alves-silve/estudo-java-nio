package br.com.server.model;

import static br.com.MainServer.DEBUG;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Classe utilizada para processar os dados recebidos do cliente.
 *
 * @author Guilherme Alves Silveira.
 */
public abstract class AbstractClientTracker<T> {

    private final boolean updateTime;
    private final long timeout;
    /**
     * Utilizado para informar, se caso mais dados do cliente cheguem, o tempo
     * de inicio é atualizado para que evite o timeout e os dados sejam
     * processados.
     */
    private final SocketChannel client;
    private final List<ByteBuffer> partRequest;
    private final List<ByteBuffer> partReadOnlyRequests;

    private int status;
    private ByteBuffer response;
    private long startTime;
    private int totalBytesLength;

    public AbstractClientTracker(long timeout, SocketChannel client) {
        this(timeout, false, client);
    }

    public AbstractClientTracker(long timeout, boolean updateTime, SocketChannel client) {
        this.timeout = timeout;
        this.updateTime = updateTime;
        this.partRequest = new ArrayList<>();
        this.partReadOnlyRequests = new ArrayList<>();
        this.startTime = System.currentTimeMillis();
        this.client = Objects.requireNonNull(client);
    }

    /**
     * @return As requisições que serão somente usadas para leitura, onde seus
     * dados não poderão ser modificados, mas poderão ser usados para
     * processamento.
     */
    public List<ByteBuffer> getPartReadOnlyRequests() {
        return Collections.unmodifiableList(partReadOnlyRequests);
    }

    public boolean isEmpty() {
        return partRequest.isEmpty();
    }

    public boolean timeoutReached() {
        return (System.currentTimeMillis() - startTime) > timeout;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        switch (status) {
            case StatusClientTracker.PROCESS:
            case StatusClientTracker.TIMEOUT:
                this.status = status;
                break;
            default:
                throw new IllegalArgumentException("Invalid status!");
        }
    }

    public boolean addRequestPart(ByteBuffer buff) {
        if (!buff.hasRemaining()) {
            throw new IllegalArgumentException("Buffer don't has data or it has to be flipped!");
        }

        if (updateTime) {
            startTime = System.currentTimeMillis();
        }

        totalBytesLength += buff.remaining();
        partReadOnlyRequests.add(buff.asReadOnlyBuffer());
        return partRequest.add(buff);
    }

    public ByteBuffer mountPartRequest(int length) {
        ByteBuffer part = ByteBuffer.allocateDirect(length);
        int realSize = 0;
        for (ByteBuffer requestPart : partReadOnlyRequests) {
            realSize += requestPart.remaining();
            ByteBuffer duplicated = requestPart.duplicate();

            if (duplicated.remaining() > length) {
                duplicated.limit(duplicated.position() + length);
            }

            part.put(duplicated);
            if (realSize >= length) {
                break;
            }
        }
        part.flip();
        return part;
    }

    public ByteBuffer mountRequest() {
        try {
            if (DEBUG) {
                int size = 0;
                for (ByteBuffer requestPart : partRequest) {
                    size += requestPart.remaining();
                }

                if (size != totalBytesLength) {
                    System.err.println("WRONG!");
                }
            }

            ByteBuffer request = ByteBuffer.allocateDirect(totalBytesLength);
            for (ByteBuffer requestPart : partRequest) {
                request.put(requestPart);
            }
            request.flip();
            return request;
        } finally {
            totalBytesLength = 0;
            partRequest.clear();
            partReadOnlyRequests.clear();
        }
    }

    public byte[] mountByteRequest() {
        ByteBuffer request = mountRequest();
        byte[] bb = new byte[request.limit()];
        request.get(bb);
        return bb;
    }

    public List<ByteBuffer> copyRequests() {
        List<ByteBuffer> copies = new ArrayList<>();
        for (ByteBuffer src : partRequest) {
            ByteBuffer dest = ByteBuffer.allocateDirect(src.capacity());
            dest.position(src.position())
                    .limit(src.limit());
            for (int i = src.position(); i < src.limit(); i++) {
                dest.put(i, src.get(i));
            }
            copies.add(dest);
        }

        return copies;
    }

    public SocketChannel getClient() {
        return client;
    }

    public int getTotalBytesLength() {
        return totalBytesLength;
    }

    public boolean isUpdateTime() {
        return updateTime;
    }

    public ByteBuffer getResponse() {
        return response;
    }

    public void setResponse(ByteBuffer response) {
        this.response = Objects.requireNonNull(response);
    }

    public void setResponse(byte[] byteResponse) {
        Objects.requireNonNull(byteResponse);
        if (null == response || response.capacity() < byteResponse.length) {
            this.response = ByteBuffer.allocateDirect(byteResponse.length);
        }
        response.clear();
        this.response.put(byteResponse);
        this.response.flip();
    }

    public long getTimeout() {
        return timeout;
    }

    public long getStartTime() {
        return startTime;
    }

    /**
     * <pre>
     * A classe que sobreescrever esse método, deverá informar se o objeto passado
     * indica que é o fim da requisição, exemplo:
     * byte[] crlf = "\r\n".getBytes();
     * if (isEndOfRequest(crlf)) {
     *  //Sucesso, agora os dados deverão ser processados.
     * } else {
     *  //O servidor continua acumulando os bytes para processar na próxima vez.
     * }
     * // Ou ...
     * int tamanhoDosDados = //...
     * if (isEndOfRequest(tamanhoDosDados)) {
     *  //Sucesso, agora os dados deverão ser processados.
     * } else {
     *  //O servidor continua acumulando os bytes para processar na próxima vez.
     * }
     * </pre>
     *
     * @param end Valor que indica que chegou a fim da requisição e que juntou
     * todas as informações necessárias para realizar o processamento.
     * @return Se o byte passado indica que chegou ao fim da requisição.
     */
    public abstract boolean isEndOfRequest(T t);

    public boolean isTimeoutReached() {
        long elapsedTime = System.currentTimeMillis() - startTime;
        return elapsedTime >= timeout;
    }

    @Override
    public String toString() {
        return "ClientTracker{"
                + "timeout=" + timeout
                + ", requests=" + partRequest
                + ", response=" + response
                + ", startTime=" + startTime
                + ", totalBytes=" + totalBytesLength
                + ", updateTime=" + updateTime
                + '}';
    }
}
