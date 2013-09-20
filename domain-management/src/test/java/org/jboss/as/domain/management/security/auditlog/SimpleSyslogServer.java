/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.domain.management.security.auditlog;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.xnio.IoUtils;

/**
 * @author: Kabir Khan
 */
public abstract class SimpleSyslogServer implements Runnable {

    protected final AtomicBoolean closed = new AtomicBoolean(false);
    protected final BlockingQueue<byte[]> receivedData = new LinkedBlockingQueue<byte[]>();

    SimpleSyslogServer(){
    }

    static SimpleSyslogServer createUdp(int port) throws IOException {
        SimpleSyslogServer server = new Udp(port);
        Thread t = new Thread(server);
        t.start();
        return server;
    }

    static SimpleSyslogServer createTcp(int port, boolean octetCounting) throws IOException {
        SimpleSyslogServer server = new Tcp(new ServerSocket(port), octetCounting);
        Thread t = new Thread(server);
        t.start();
        return server;
    }

    static SimpleSyslogServer createTls(int port, boolean octetCounting, File keystorePath, String keystorePassword, File clientCertPath, String clientCertPwd) throws IOException, GeneralSecurityException {
        KeyManager[] keyManagers;
        final KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        final FileInputStream in = new FileInputStream(keystorePath);
        try {
            final KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(in, keystorePassword.toCharArray());
            kmf.init(ks, keystorePassword.toCharArray());
            keyManagers = kmf.getKeyManagers();
        } finally {
            IoUtils.safeClose(in);
        }
        boolean requireClientAuth = false;
        TrustManager[] trustManagers = null;
        if (clientCertPath != null) {
            requireClientAuth = true;
            final TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            final FileInputStream clientIn = new FileInputStream(clientCertPath);
            try {
                final KeyStore ks = KeyStore.getInstance("JKS");
                ks.load(clientIn, clientCertPwd.toCharArray());
                tmf.init(ks);
                trustManagers = tmf.getTrustManagers();
            } finally {
                IoUtils.safeClose(in);
            }
        }

        final SSLContext context = SSLContext.getInstance("TLS");
        context.init(keyManagers, trustManagers, null);

        SSLServerSocket socket = (SSLServerSocket)context.getServerSocketFactory().createServerSocket(port);
        socket.setNeedClientAuth(requireClientAuth);
        SimpleSyslogServer server = new Tcp(socket, octetCounting);

        Thread t = new Thread(server);
        t.start();
        return server;
    }
    abstract void close();

    byte[] receiveData() throws InterruptedException {
        return receivedData.poll(20, TimeUnit.SECONDS);
    }

    byte[] pollData() {
        return receivedData.poll();
    }

    private static class Udp extends SimpleSyslogServer {

        private final DatagramSocket socket;

        Udp(int port) throws IOException {
            socket = new DatagramSocket(port);
        }

        @Override
        void close() {
            closed.set(true);
            socket.close();
        }

        @Override
        public void run(){

            while (!closed.get()){
                try {
                    DatagramPacket packet = new DatagramPacket(new byte[2048], 2048);
                    socket.receive(packet);
                    byte[] bytes = new byte[packet.getLength()];
                    System.arraycopy(packet.getData(), 0, bytes, 0, packet.getLength());
                    receivedData.add(bytes);
                } catch (IOException e) {
                    if (!closed.get()){
                        e.printStackTrace();
                        close();
                    }
                }
            }
        }
    }

    private static class Tcp extends SimpleSyslogServer {
        private final boolean octetCounting;
        private final ServerSocket serverSocket;
        private volatile Socket socket;

        Tcp(ServerSocket servetSocket, boolean octetCounting){
            this.serverSocket = servetSocket;
            this.octetCounting = octetCounting;
        }

        @Override
        void close() {
            closed.set(true);
            IoUtils.safeClose(serverSocket);
            IoUtils.safeClose(socket);
        }

        Socket accept(ServerSocket serverSocket) throws IOException {
            return serverSocket.accept();
        }

        @Override
        public void run() {
            try {
                socket = accept(serverSocket);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            try {
                InputStream in = new BufferedInputStream(socket.getInputStream());
                while (!closed.get()){
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    if (octetCounting) {
                        //There will be some bytes indicating the length of the message, a space and then the message
                        int i = in.read();
                        int length = -1;
                        int count = 0;
                        while (i != -1 && (length == -1 || count++ <= length)){
                            if (length == -1){
                                if ((char)i == ' '){
                                    byte[] bytes = out.toByteArray();
                                    length = 0;
                                    for (int j = 0 ; j < bytes.length ; j++) {
                                        length = length * 10 + bytes[j] - (int)Character.valueOf('0');
                                    }
                                    out.reset();
                                    continue;
                                }
                            }
                            out.write(i);
                            if (length != -1 && count > length) {
                                break;
                            }
                            i = in.read();
                        }
                    } else {
                        //Here the message is terminated by a '\n'. This means that multiline messages will appear as separate
                        //messags in syslog. For the purposes of this test, since we are using json as the output format the
                        //message will look something like
                        //      2013-05-30T23:11:52.950+01:00 Kabirs-MacBook-Pro.local WildFly 615 -  - 2013-05-30 23:11:52 - {\n....}\n.
                        //So we count the curly braces to figure out the full message
                        int braceCount = 0;
                        int i = in.read();
                        while (i != -1) {
                            out.write(i);
                            if (((char)i) == '{' ) {
                                braceCount++;
                                break;
                            }
                            i = in.read();
                        }
                        i = in.read();
                        while (i != - 1) {
                            char c = (char)i;
                            if (c == '{') {
                                braceCount++;
                            }
                            if (c == '}') {
                                braceCount--;
                            }
                            out.write(i);
                            if (c == '\n' && braceCount == 0) {
                                break;
                            }
                            i = in.read();
                        }
                    }
                    if (out.toByteArray() != null && out.toByteArray().length  > 0) {
                        receivedData.add(out.toByteArray());
                    }
                }
            } catch (IOException e) {
                if (!closed.get()){
                    e.printStackTrace();
                    close();
                }
            }
        }
    }
//
//    private static class Tls extends Tcp {
//        private final boolean requireClientAuth;
//        Tls(ServerSocket servetSocket, boolean octetCounting, boolean requireClientAuth) throws IOException {
//            super(servetSocket, octetCounting);
//            this.requireClientAuth = requireClientAuth;
//        }
//
//        Socket accept(ServerSocket serverSocket) throws IOException {
//            SSLSocket socket = (SSLSocket)serverSocket.accept();
//            socket.setNeedClientAuth(requireClientAuth);
//            return socket;
//        }
//
//    }

}
