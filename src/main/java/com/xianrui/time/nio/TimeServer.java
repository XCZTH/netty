package com.xianrui.time.nio;

public class TimeServer {

    public static void main(String[] args) {
        int port = 8080;
        if (args != null && args.length > 0) {
            try {
                port = Integer.valueOf(args[0]);
            } catch (NumberFormatException e) {
                //采用默认端口8080
            }
        }

        //独立的线程，多路复用类,轮询多路复用器Selector,可以处理多个客户端的并发接入
        MultiplexerTimeServer timeServer = new MultiplexerTimeServer(port);
        new Thread(timeServer, "NIO-MultiplexerTimeServer-001").start();
    }

}
