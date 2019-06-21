package br.com;

import br.com.server.AbstractServerController;
import br.com.server.impl.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;

/**
 * @author Guilherme Alves Silveira
 * @author Francisco Araujo
 */
public class MainServer {

    public static final boolean DEBUG = true;
    private static final int TIMEOUT = 5_000;
    private static final int BUFF_SIZE = 5;

    public static void main(String[] args) {
        try {
            /*Criando um servidor socket e configurando*/
            ServerSocketChannel server = ServerSocketChannel.open();
            server.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            server.bind(new InetSocketAddress("localhost", 5542));
            server.configureBlocking(false);
            /*--fim--*/

            //criando um selector e resgistrando no servidor
            Selector selector = Selector.open();
            /*fim*/
            AbstractServerController controller = new TimeoutServerController(TIMEOUT, BUFF_SIZE, server, selector);
            controller.runEventLoop();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
