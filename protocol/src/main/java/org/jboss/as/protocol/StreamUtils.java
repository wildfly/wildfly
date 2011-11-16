/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.protocol;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Unmarshaller;

import javax.xml.stream.XMLStreamWriter;

import static org.jboss.as.protocol.ProtocolLogger.ROOT_LOGGER;
import static org.jboss.as.protocol.ProtocolMessages.MESSAGES;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class StreamUtils {

    private StreamUtils() {
        //
    }

    public static void copyStream(final InputStream in, final OutputStream out) throws IOException {
        final byte[] bytes = new byte[8192];
        int cnt;
        while ((cnt = in.read(bytes)) != -1) {
            out.write(bytes, 0, cnt);
        }
    }
    public static void safeClose(final Closeable closeable) {
        if (closeable != null) try {
            closeable.close();
        } catch (Throwable t) {
            // temporarily disable log message until fixed in remoting/xnio
            // ROOT_LOGGER.failedToCloseResource(t, closeable);

//        java.lang.IllegalStateException
//              at org.xnio.Buffers$3.getResource(Buffers.java:1763) [xnio-api-3.0.0.CR5.jar:]
//              at org.xnio.Buffers$3.getResource(Buffers.java:1749) [xnio-api-3.0.0.CR5.jar:]
//              at org.xnio.streams.BufferPipeOutputStream.send(BufferPipeOutputStream.java:104) [xnio-api-3.0.0.CR5.jar:]
//              at org.xnio.streams.BufferPipeOutputStream.flush(BufferPipeOutputStream.java:131) [xnio-api-3.0.0.CR5.jar:]
//              at org.jboss.remoting3.remote.OutboundMessage.flush(OutboundMessage.java:173) [jboss-remoting-3.2.0.CR4.jar:]
//              at java.io.DataOutputStream.flush(DataOutputStream.java:106) [:1.6.0_26]
//              at java.io.FilterOutputStream.close(FilterOutputStream.java:140) [:1.6.0_26]
//              at org.jboss.as.protocol.mgmt.FlushableDataOutputImpl2.close(FlushableDataOutputImpl2.java:120) [jboss-as-protocol-7.1.0.CR1-SNAPSHOT.jar:]
//              at org.jboss.as.protocol.StreamUtils.safeClose(StreamUtils.java:59) [jboss-as-protocol-7.1.0.CR1-SNAPSHOT.jar:]
//              at org.jboss.as.controller.remote.ModelControllerClientOperationHandler$ExecuteRequestHandler$1.execute(ModelControllerClientOperationHandler.java:107) [jboss-as-controller-7.1.0.CR1-SNAPSHOT.jar:]
//              at org.jboss.as.protocol.mgmt.AbstractMessageHandler$2$1.run(AbstractMessageHandler.java:252) [jboss-as-protocol-7.1.0.CR1-SNAPSHOT.jar:]
//              at java.util.concurrent.ThreadPoolExecutor$Worker.runTask(ThreadPoolExecutor.java:886) [:1.6.0_26]
//              at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:908) [:1.6.0_26]
//              at java.lang.Thread.run(Thread.java:662) [:1.6.0_26]

        }
    }

    public static void safeClose(final Socket socket) {
        if (socket != null) try {
            socket.close();
        } catch (Throwable t) {
            ROOT_LOGGER.failedToCloseResource(t, socket);
        }
    }

    public static void safeClose(final ServerSocket serverSocket) {
        if (serverSocket != null) try {
            serverSocket.close();
        } catch (IOException e) {
            ROOT_LOGGER.failedToCloseServerSocket(e, serverSocket);
        }
    }

    public static void safeClose(final XMLStreamWriter writer) {
        if (writer != null) try {
            writer.close();
        } catch (Throwable t) {
            ROOT_LOGGER.failedToCloseResource(t, writer);
        }
    }
}
