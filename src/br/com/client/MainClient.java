package br.com.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Guilherme Alves Silveira
 * @author Francisco Araujo
 */
public class MainClient {

    public static final String FIXED_LENGTH = "FIXED_LENGTH";
    public static final String VARIABLE_LENGTH = "VARIABLE_LENGTH";
    public static final String TIMEOUT = "TIMEOUT";
    public static final String END = "END";
    private static final AtomicInteger POS = new AtomicInteger();
    
    public static void main(String... args) throws IOException, InterruptedException {
        String opt = args.length > 0? args[0] : TIMEOUT;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        for (int i = 0; i < 10; i++) {
            executor.execute(() -> sendSizeRequest(opt));
        }
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.DAYS);
    }

    private static void sendSizeRequest(String opt) {
        InetSocketAddress crunchifyAddr = new InetSocketAddress("localhost", 5542);
        try (SocketChannel crunchifyClient = SocketChannel.open(crunchifyAddr)) {
            crunchifyClient.configureBlocking(true);
            List<String> toProcess;
            switch (Objects.requireNonNull(opt)) {
                case FIXED_LENGTH:
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < 64; i++) {
                        sb.append("1");
                    }
                    toProcess = Arrays.asList(sb.toString());
                    break;
                case VARIABLE_LENGTH:
                    String headerBodySize = "10000";
                    int size = Integer.parseInt(headerBodySize);
                    StringBuilder body = new StringBuilder();
                    for (int i = 0; i < size; i++) {
                        body.append("1");
                    }
                    toProcess = Arrays.asList(headerBodySize + body);
                    break;
                case TIMEOUT:
                    toProcess = Arrays.asList(
                            "Company 1\r\nCompany 2\r\n"
                            + "Company 3\r\nCompany 4\r\n"
                            + "Company 5\r\nCompany 6\r\n"
                            + "Company 7\r\nCompany 8\r\n"
                            + "Company 9\r\nCompany 10\r\n"
                            + "Company 11\r\n,Company 12\r\n");
                    break;
                case END:
                    toProcess = Arrays.asList(
                            "Company 1Company 2"
                            + "Company 3Company 4"
                            + "Company 5Company 6"
                            + "Company 7Company 8"
                            + "Company 9Company 10"
                            + "Company 11,Company 12\r\n");
                    break;
                default:
                    throw new IllegalArgumentException("Invalid operation [" + opt + "]");
            }

            int pos = POS.incrementAndGet();
            for (String datum : toProcess) {
                byte[] message = datum.getBytes();
                ByteBuffer buffer = ByteBuffer.wrap(message);
                crunchifyClient.write(buffer);
                if ("TIMEOUT".equals(opt)) {
                    Thread.sleep(7000);
                }
                ByteBuffer buff = ByteBuffer.allocate(1024);
                crunchifyClient.read(buff);
                buff.flip();
                System.out.println(pos + "- " + new String(buff.array()));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
