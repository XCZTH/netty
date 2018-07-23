package com.xianrui.time.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

public class MultiplexerTimeServer implements Runnable {
    private Selector selector;

    private ServerSocketChannel servChannel;

    private volatile boolean stop;


    /**
     * 初始化多路复用器，绑定监听端口
     *
     * @param port
     */
    public MultiplexerTimeServer(int port) {
        try {
            selector = Selector.open();
            servChannel = ServerSocketChannel.open();//打开ServerSocketChannel,用于监听客户端的连接
            servChannel.configureBlocking(false);//设置连接为非阻塞模式
            servChannel.socket().bind(new InetSocketAddress(port), 1024);//绑定监听端口
            servChannel.register(selector, SelectionKey.OP_ACCEPT);//将ServerSocketChannel注册到多路复用器，监听ACCEPT事件
            System.out.println("The time server is start in port:" + port);

        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);

        }
    }

    public void stop() {
        this.stop = true;
    }

    @Override
    public void run() {
        while (!stop) {
            try {
                selector.select(1000);//无论是否有读写等事件发生，每隔1s都被唤醒一次
                Set<SelectionKey> selectionKeys = selector.selectedKeys();
                Iterator<SelectionKey> it = selectionKeys.iterator();
                SelectionKey key = null;
                while (it.hasNext()) {
                    key = it.next();
                    it.remove();
                    try {
                        handleInput(key);
                    } catch (Exception e) {
                        if (key != null) {
                            key.cancel();
                            if (key.channel() != null) {
                                key.channel().close();
                            }
                        }
                    }
                }

            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        if (selector != null) {
            try {
                selector.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleInput(SelectionKey key) throws IOException {
        if (key.isValid()) {
            if (key.isAcceptable()) {
                ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                SocketChannel sc = ssc.accept();
                sc.configureBlocking(false);
                sc.register(selector, SelectionKey.OP_READ);
            }

            if (key.isReadable()) {
                SocketChannel sc = (SocketChannel) key.channel();
                ByteBuffer readBuffer = ByteBuffer.allocate(1024);//分配缓冲区
                int readBytes = sc.read(readBuffer);
                if (readBytes > 0) {
                    readBuffer.flip();//当前limit设置为了position，position设置为0
                    byte[] bytes = new byte[readBuffer.remaining()];
                    readBuffer.get(bytes); //将缓冲区可读的字符数组复制到新创建的字节数组中
                    String body = new String(bytes, "UTF-8");
                    System.out.println("The time server receive order : " + body);
                    String currentTime = "QUERY TIME ORDER".equalsIgnoreCase(body) ? new Date(System.currentTimeMillis())
                            .toString() : "BAD ORDER";
                    doWrite(sc, currentTime);
                } else if (readBytes < 0) {
                    //对端链路的关闭
                    key.cancel();
                    sc.close();
                } else
                    ;//读到0字节，忽略

            }
        }
    }

    private void doWrite(SocketChannel channel, String response) throws IOException {
        if (response != null && response.trim().length() > 0) {
            byte[] bytes = response.getBytes();
            ByteBuffer writeBuffer = ByteBuffer.allocate(bytes.length);
            writeBuffer.put(bytes);//将字节数组复制到缓冲区中
            writeBuffer.flip();//当前limit设置为了position，position设置为0，表示读操作从缓存头开始读
            channel.write(writeBuffer);//将缓冲区的字节数组发送出去
        }
    }
}
