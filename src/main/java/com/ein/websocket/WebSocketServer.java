package com.ein.websocket;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;

/**
 * Created by JiangFeng on 2016/6/21.
 */

public class WebSocketServer {
    private ServerSocket serverSocket;
    private int i=0;
    public WebSocketServer() throws IOException {
        serverSocket = new ServerSocket(9880);
        System.out.println("服务器启动");
    }
    public void service() {
        Socket socket = null;
        while (true) {
            try {
                socket = serverSocket.accept();
                Thread workThread = new Thread(new Handler(socket));
                workThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    class Handler implements Runnable {
        private Socket socket;
        private boolean hasHandshake = false;
        Charset charset = Charset.forName("UTF-8");
        public Handler(Socket socket) {
            this.socket = socket;
        }
        public void run() {
            try {
                System.out.println("client["+(i++)+"] connected: " + socket.getInetAddress() + ":" + socket.getPort());
                InputStream in = socket.getInputStream();
                PrintWriter pw = new PrintWriter(socket.getOutputStream(), true);
                byte[] buf = new byte[1024];
                int len = in.read(buf, 0, 1024);
                byte[] res = new byte[len];
                System.arraycopy(buf, 0, res, 0, len);
                String key = new String(res);
                if (!hasHandshake && key.indexOf("Key") > 0) { // 握手
                    key = key.substring(0, key.indexOf("==") + 2);
                    key = key.substring(key.indexOf("Key") + 4, key.length()).trim();
                    key += "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
                    MessageDigest md = MessageDigest.getInstance("SHA-1");
                    md.update(key.getBytes("utf-8"), 0, key.length());
                    byte[] sha1Hash = md.digest();
                    sun.misc.BASE64Encoder encoder = new sun.misc.BASE64Encoder();
                    key = encoder.encode(sha1Hash);
                    pw.println("HTTP/1.1 101 Switching Protocols");
                    pw.println("Upgrade: websocket");
                    pw.println("Connection: Upgrade");
                    pw.println("Sec-WebSocket-Accept: " + key);
                    pw.println();
                    pw.flush();
                    hasHandshake = true;
                    // 接收数据
                    byte[] first = new byte[1];
                    int read = in.read(first, 0, 1); // 这里会阻塞
                    while (read > 0) {
                        int b = first[0] & 0xFF;
                        // 1为字符数据，8为关闭socket
                        byte opCode = (byte) (b & 0x0F);
                        if (opCode == 8) {
                            socket.getOutputStream().close();
                            break;
                        }
                        b = in.read();
                        int payloadLength = b & 0x7F;
                        if (payloadLength == 126) {
                            byte[] extended = new byte[2];
                            in.read(extended, 0, 2);
                            int shift = 0;
                            payloadLength = 0;
                            for (int i = extended.length - 1; i >= 0; i--) {
                                payloadLength = payloadLength + ((extended[i] & 0xFF) << shift);
                                shift += 8;
                            }
                        } else if (payloadLength == 127) {
                            byte[] extended = new byte[8];
                            in.read(extended, 0, 8);
                            int shift = 0;
                            payloadLength = 0;
                            for (int i = extended.length - 1; i >= 0; i--) {
                                payloadLength = payloadLength
                                        + ((extended[i] & 0xFF) << shift);
                                shift += 8;
                            }
                        }
                        // 掩码
                        byte[] mask = new byte[4];
                        in.read(mask, 0, 4);
                        int readThisFragment = 1;
                        ByteBuffer byteBuf = ByteBuffer.allocate(payloadLength + 10);
                        byteBuf.put("echo: ".getBytes("UTF-8"));
                        while (payloadLength > 0) {
                            int masked = in.read();
                            masked = masked ^ (mask[(int) ((readThisFragment - 1) % 4)] & 0xFF);
                            byteBuf.put((byte) masked);
                            payloadLength--;
                            readThisFragment++;
                        }
                        byteBuf.flip();
                        responseClient(byteBuf, true);
                        printRes(byteBuf.array());
                        in.read(first, 0, 1);
                    }
                }
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (socket != null){
                        System.out.println("client["+(i--)+"] disconnected: "+ socket.getInetAddress() + ":" + socket.getPort());
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void responseClient(ByteBuffer byteBuf, boolean finalFragment) throws IOException {
            OutputStream out = socket.getOutputStream();
            int first = 0x00;
            if (finalFragment) {// 是否是输出最后的WebSocket响应片段
                first = first + 0x80;
                first = first + 0x1;
            }
            out.write(first);

            if (byteBuf.limit() < 126) {
                out.write(byteBuf.limit());
            } else if (byteBuf.limit() < 65536) {
                out.write(126);
                out.write(byteBuf.limit() >>> 8);
                out.write(byteBuf.limit() & 0xFF);
            } else {
                // Will never be more than 2^31-1
                out.write(127);
                out.write(0);
                out.write(0);
                out.write(0);
                out.write(0);
                out.write(byteBuf.limit() >>> 24);
                out.write(byteBuf.limit() >>> 16);
                out.write(byteBuf.limit() >>> 8);
                out.write(byteBuf.limit() & 0xFF);
            }
            // Write the content
            out.write(byteBuf.array(), 0, byteBuf.limit());
            out.flush();
        }
        private void printRes(byte[] array) {
            ByteArrayInputStream byteIn = new ByteArrayInputStream(array);
            InputStreamReader reader = new InputStreamReader(byteIn,charset.newDecoder());
            StringBuilder res = new StringBuilder();
            try {
                int b = 0;
                while ((b = reader.read()) > 0) {
                    res.append((char) b);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println(res.toString());
        }
    }

}

