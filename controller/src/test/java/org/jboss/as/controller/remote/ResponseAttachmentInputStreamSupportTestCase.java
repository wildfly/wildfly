/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.remote;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.Executor;

import org.jboss.as.controller.client.OperationResponse;
import org.jboss.as.controller.client.impl.ModelControllerProtocol;
import org.jboss.as.protocol.mgmt.ActiveOperation;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementProtocol;
import org.jboss.as.protocol.mgmt.ManagementProtocolHeader;
import org.jboss.as.protocol.mgmt.ManagementRequestContext;
import org.jboss.as.protocol.mgmt.ManagementRequestHandler;
import org.jboss.as.protocol.mgmt.ManagementRequestHeader;
import org.jboss.remoting3.Channel;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests of {@link org.jboss.as.controller.remote.ResponseAttachmentInputStreamSupport}.
 *
 * @author Brian Stansberry (c) 2014 Red Hat Inc.
 */
public class ResponseAttachmentInputStreamSupportTestCase {

    private static final byte[] data = new byte[8193];

    @Test
    public void testReadHandler() throws IOException {

        ResponseAttachmentInputStreamSupport testee = new ResponseAttachmentInputStreamSupport();
        InputStream stream = new ByteArrayInputStream(data);
        OperationResponse.StreamEntry streamEntry = new MockStreamEntry(stream);
        testee.registerStreams(1, Arrays.asList(streamEntry));

        ManagementRequestHandler<Void, Void> handler = testee.getReadHandler();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MockFlushableDataOutput mfdo = new MockFlushableDataOutput(baos);
        ActiveOperation.ResultHandler<Void> mrh = new MockResultHandler();
        handler.handleRequest(getDataInput(1, 0), mrh, new MockManagementRequestContext(mfdo));

        Assert.assertEquals(1, ((MockStreamEntry) streamEntry).closeCount);
        Assert.assertTrue("Close count: " + mfdo.closeCount, mfdo.closeCount > 0);
        Assert.assertEquals(1, ((MockResultHandler) mrh).result);

        DataInput di = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));

        Assert.assertEquals(ModelControllerProtocol.PARAM_INPUTSTREAM_LENGTH, di.readByte());
        int length = di.readInt();
        Assert.assertEquals(8192, length);
        Assert.assertEquals(ModelControllerProtocol.PARAM_INPUTSTREAM_CONTENTS, di.readByte());
        di.readFully(new byte[length]);
        Assert.assertEquals(ModelControllerProtocol.PARAM_INPUTSTREAM_LENGTH, di.readByte());
        length = di.readInt();
        Assert.assertEquals(1, length);
        Assert.assertEquals(ModelControllerProtocol.PARAM_INPUTSTREAM_CONTENTS, di.readByte());
        di.readFully(new byte[length]);
        Assert.assertEquals(ModelControllerProtocol.PARAM_END, di.readByte());
        Assert.assertEquals(ManagementProtocol.RESPONSE_END, di.readByte());

        // Test a missing entry
        baos = new ByteArrayOutputStream();
        mfdo = new MockFlushableDataOutput(baos);
        handler.handleRequest(getDataInput(1, 0), mrh, new MockManagementRequestContext(mfdo));

        Assert.assertEquals(1, ((MockStreamEntry) streamEntry).closeCount);
        Assert.assertTrue("Close count: " + mfdo.closeCount, mfdo.closeCount > 0);
        Assert.assertEquals(2, ((MockResultHandler) mrh).result);

        di = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));

        Assert.assertEquals(ModelControllerProtocol.PARAM_END, di.readByte());
        Assert.assertEquals(ManagementProtocol.RESPONSE_END, di.readByte());
    }

    @Test
    public void testCloseHandler() throws IOException {

        ResponseAttachmentInputStreamSupport testee = new ResponseAttachmentInputStreamSupport();
        InputStream stream = new ByteArrayInputStream(data);
        OperationResponse.StreamEntry streamEntry = new MockStreamEntry(stream);
        testee.registerStreams(1, Arrays.asList(streamEntry));

        ManagementRequestHandler<Void, Void> handler = testee.getCloseHandler();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MockFlushableDataOutput mfdo = new MockFlushableDataOutput(baos);
        ActiveOperation.ResultHandler<Void> mrh = new MockResultHandler();
        handler.handleRequest(getDataInput(1, 0), mrh, new MockManagementRequestContext(mfdo));

        Assert.assertEquals(1, ((MockStreamEntry) streamEntry).closeCount);
        Assert.assertTrue("Close count: " + mfdo.closeCount, mfdo.closeCount > 0);
        Assert.assertEquals(1, ((MockResultHandler) mrh).result);

        DataInput di = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
        Assert.assertEquals(ManagementProtocol.RESPONSE_END, di.readByte());

        // Test a missing entry
        baos = new ByteArrayOutputStream();
        mfdo = new MockFlushableDataOutput(baos);
        handler.handleRequest(getDataInput(1, 0), mrh, new MockManagementRequestContext(mfdo));

        Assert.assertEquals(1, ((MockStreamEntry) streamEntry).closeCount);  // still only closed once because it wasn't registered
        Assert.assertTrue("Close count: " + mfdo.closeCount, mfdo.closeCount > 0);
        Assert.assertEquals(2, ((MockResultHandler) mrh).result);

        di = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
        Assert.assertEquals(ManagementProtocol.RESPONSE_END, di.readByte());
    }

    @Test
    public void testShutdown() throws IOException {

        ResponseAttachmentInputStreamSupport testee = new ResponseAttachmentInputStreamSupport();
        InputStream stream1 = new ByteArrayInputStream(data);
        OperationResponse.StreamEntry streamEntry1 = new MockStreamEntry(stream1);
        InputStream stream2 = new ByteArrayInputStream(data);
        OperationResponse.StreamEntry streamEntry2 = new MockStreamEntry(stream2);
        testee.registerStreams(1, Arrays.asList(streamEntry1, streamEntry2));

        testee.shutdown();

        Assert.assertEquals(1, ((MockStreamEntry) streamEntry1).closeCount);
        Assert.assertEquals(1, ((MockStreamEntry) streamEntry2).closeCount);

        // Validate that streams registered after shutdown are immediately closed and are not usable by a caller
        testee.registerStreams(2, Arrays.asList(streamEntry1, streamEntry2));

        Assert.assertEquals(2, ((MockStreamEntry) streamEntry1).closeCount); // stream was closed by registerStreams
        Assert.assertEquals(2, ((MockStreamEntry) streamEntry2).closeCount); // stream was closed by registerStreams

        // Validate readHandler treats as empty stream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MockFlushableDataOutput mfdo = new MockFlushableDataOutput(baos);
        ActiveOperation.ResultHandler<Void> mrh = new MockResultHandler();
        testee.getReadHandler().handleRequest(getDataInput(2, 0), mrh, new MockManagementRequestContext(mfdo));

        Assert.assertEquals(2, ((MockStreamEntry) streamEntry1).closeCount);
        Assert.assertTrue("Close count: " + mfdo.closeCount, mfdo.closeCount > 0);
        Assert.assertEquals(1, ((MockResultHandler) mrh).result);

        DataInput di = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
        Assert.assertEquals(ModelControllerProtocol.PARAM_END, di.readByte());
        Assert.assertEquals(ManagementProtocol.RESPONSE_END, di.readByte());

        baos = new ByteArrayOutputStream();
        mfdo = new MockFlushableDataOutput(baos);
        testee.getCloseHandler().handleRequest(getDataInput(2, 1), mrh, new MockManagementRequestContext(mfdo));

        Assert.assertEquals(2, ((MockStreamEntry) streamEntry2).closeCount);
        Assert.assertTrue("Close count: " + mfdo.closeCount, mfdo.closeCount > 0);
        Assert.assertEquals(2, ((MockResultHandler) mrh).result);

        di = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
        Assert.assertEquals(ManagementProtocol.RESPONSE_END, di.readByte());
    }

    @Test
    public void testGC() throws IOException, InterruptedException {

        ResponseAttachmentInputStreamSupport testee = new ResponseAttachmentInputStreamSupport(1);
        InputStream stream1 = new ByteArrayInputStream(data);
        OperationResponse.StreamEntry streamEntry1 = new MockStreamEntry(stream1);
        InputStream stream2 = new ByteArrayInputStream(data);
        OperationResponse.StreamEntry streamEntry2 = new MockStreamEntry(stream2);
        testee.registerStreams(1, Arrays.asList(streamEntry1, streamEntry2));

        Thread.sleep(2);

        testee.gc();

        Assert.assertEquals(1, ((MockStreamEntry) streamEntry1).closeCount);
        Assert.assertEquals(1, ((MockStreamEntry) streamEntry2).closeCount);

        // Validate readHandler treats as empty stream
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MockFlushableDataOutput mfdo = new MockFlushableDataOutput(baos);
        ActiveOperation.ResultHandler<Void> mrh = new MockResultHandler();
        testee.getReadHandler().handleRequest(getDataInput(1, 0), mrh, new MockManagementRequestContext(mfdo));

        Assert.assertEquals(1, ((MockStreamEntry) streamEntry1).closeCount);
        Assert.assertTrue("Close count: " + mfdo.closeCount, mfdo.closeCount > 0);
        Assert.assertEquals(1, ((MockResultHandler) mrh).result);

        DataInput di = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
        Assert.assertEquals(ModelControllerProtocol.PARAM_END, di.readByte());
        Assert.assertEquals(ManagementProtocol.RESPONSE_END, di.readByte());

        baos = new ByteArrayOutputStream();
        mfdo = new MockFlushableDataOutput(baos);
        testee.getCloseHandler().handleRequest(getDataInput(1, 1), mrh, new MockManagementRequestContext(mfdo));

        Assert.assertEquals(1, ((MockStreamEntry) streamEntry2).closeCount);
        Assert.assertTrue("Close count: " + mfdo.closeCount, mfdo.closeCount > 0);
        Assert.assertEquals(2, ((MockResultHandler) mrh).result);

        di = new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
        Assert.assertEquals(ManagementProtocol.RESPONSE_END, di.readByte());
    }

    private static DataInput getDataInput(int operationId, int streamIndex) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeByte(ModelControllerProtocol.PARAM_OPERATION);
        dos.writeInt(operationId);
        dos.writeByte(ModelControllerProtocol.PARAM_INPUTSTREAM_INDEX);
        dos.writeInt(streamIndex);
        dos.flush();
        return new DataInputStream(new ByteArrayInputStream(baos.toByteArray()));
    }

    private static final class MockFlushableDataOutput extends DataOutputStream implements FlushableDataOutput {

        private int closeCount;

        /**
         * Creates a new data output stream to write data to the specified
         * underlying output stream. The counter <code>written</code> is
         * set to zero.
         *
         * @param out the underlying output stream, to be saved for later
         *            use.
         * @see java.io.FilterOutputStream#out
         */
        public MockFlushableDataOutput(OutputStream out) {
            super(out);
        }

        @Override
        public void close() throws IOException {
            super.close();
            closeCount++;
        }
    }

    private static final class MockManagementRequestContext implements ManagementRequestContext<Void> {

        private final FlushableDataOutput dataOutput;

        private MockManagementRequestContext(FlushableDataOutput dataOutput) {
            this.dataOutput = dataOutput;
        }

        @Override
        public Integer getOperationId() {
            return 1;
        }

        @Override
        public Void getAttachment() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Channel getChannel() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ManagementProtocolHeader getRequestHeader() {
            return new ManagementRequestHeader(1, 1, 1, (byte) 1);
        }

        @Override
        public void executeAsync(AsyncTask<Void> task) {
            try {
                task.execute(this);
            } catch (RuntimeException r) {
                throw r;
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void executeAsync(AsyncTask<Void> task, boolean cancellable) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void executeAsync(AsyncTask<Void> task, Executor executor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void executeAsync(AsyncTask<Void> task, boolean cancellable, Executor executor) {
            throw new UnsupportedOperationException();
        }

        @Override
        public FlushableDataOutput writeMessage(ManagementProtocolHeader header) throws IOException {
            return dataOutput;
        }
    }

    private static class MockResultHandler implements ActiveOperation.ResultHandler<Void> {

        private volatile int result;
        @Override
        public boolean done(Void result) {
            int was = this.result;
            this.result += 1;
            return was == 0;
        }

        @Override
        public boolean failed(Exception e) {
            int was = this.result;
            this.result += 10;
            return was == 0;
        }

        @Override
        public void cancel() {
            this.result += 100;
        }
    }

    private static final class MockStreamEntry implements OperationResponse.StreamEntry {

        private final InputStream stream;
        private int closeCount;

        private MockStreamEntry(InputStream stream) {
            this.stream = stream;
        }

        @Override
        public String getUUID() {
            return toString();
        }

        @Override
        public String getMimeType() {
            return "application/x-www-form-urlencoded";
        }

        @Override
        public InputStream getStream() {
            return stream;
        }

        @Override
        public void close() throws IOException {
            stream.close();
            closeCount++;
        }
    }
}
