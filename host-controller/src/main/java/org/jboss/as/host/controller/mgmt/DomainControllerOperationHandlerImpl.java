/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.host.controller.mgmt;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.protocol.ProtocolUtils.expectHeader;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ControllerTransactionContext;
import org.jboss.as.controller.HashUtil;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.client.ExecutionContext;
import org.jboss.as.controller.client.ModelControllerClientProtocol;
import org.jboss.as.controller.remote.ModelControllerOperationHandlerImpl;
import org.jboss.as.controller.remote.RemoteProxyController;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.domain.controller.HostControllerClient;
import org.jboss.as.protocol.ByteDataInput;
import org.jboss.as.protocol.ByteDataOutput;
import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.MessageHandler;
import org.jboss.as.protocol.SimpleByteDataInput;
import org.jboss.as.protocol.SimpleByteDataOutput;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementRequestConnectionStrategy;
import org.jboss.as.protocol.mgmt.ManagementResponse;
import org.jboss.dmr.ModelNode;

/**
 * Standard ModelController operation handler that also has the operations for HC->DC.
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class DomainControllerOperationHandlerImpl extends ModelControllerOperationHandlerImpl {

    public static final byte HANDLER_ID = (byte)0x10;
    public static final byte EXECUTE_TRANSACTIONAL_REQUEST = (byte)0x21;
    public static final byte EXECUTE_TRANSACTIONAL_RESPONSE = (byte)0x22;
    public static final byte TRANSACTION_COMMIT_REQUEST = (byte)0x23;
    public static final byte TRANSACTION_COMMIT_RESPONSE = (byte)0x24;
    public static final byte TRANSACTION_ROLLBACK_REQUEST = (byte)0x25;
    public static final byte TRANSACTION_ROLLBACK_RESPONSE = (byte)0x26;
    public static final byte TRANSACTION_ID = (byte)0x30;

    public DomainControllerOperationHandlerImpl(DomainController modelController, MessageHandler initiatingHandler) {
        super(modelController, initiatingHandler);
    }

    @Override
    protected DomainController getController() {
        return (DomainController)super.getController();
    }

    @Override
    public ManagementResponse operationFor(byte commandByte) {
        switch (commandByte) {
        case DomainControllerProtocol.REGISTER_HOST_CONTROLLER_REQUEST:
            return new RegisterOperation();
        case DomainControllerProtocol.UNREGISTER_HOST_CONTROLLER_REQUEST:
            return new UnregisterOperation();
        case DomainControllerProtocol.GET_FILE_REQUEST:
            return new GetFileOperation();
        default:
            return super.operationFor(commandByte);
        }
    }

    private abstract class RegistryOperation extends ManagementResponse {
        String hostId;

        RegistryOperation() {
            super(getInitiatingHandler());
        }

        @Override
        protected void readRequest(final InputStream inputStream) throws IOException {
            expectHeader(inputStream, DomainControllerProtocol.PARAM_HOST_ID);
            hostId = StreamUtils.readUTFZBytes(inputStream);
        }
    }

    private class RegisterOperation extends RegistryOperation {
        Connection connection;
        @Override
        protected final byte getResponseCode() {
            return DomainControllerProtocol.REGISTER_HOST_CONTROLLER_RESPONSE;
        }

        @Override
        public void handle(final Connection connection, final InputStream input) throws IOException {
            this.connection = connection;
            super.handle(connection, input);
        }


        @Override
        protected void sendResponse(final OutputStream outputStream) throws IOException {
            getController().addClient(new RemoteHostControllerClient(hostId, connection));
            ModelNode node = getController().getDomainModel();
            outputStream.write(DomainControllerProtocol.PARAM_MODEL);
            node.writeExternal(outputStream);
        }
    }

    private class UnregisterOperation extends RegistryOperation {
        @Override
        protected final byte getResponseCode() {
            return DomainControllerProtocol.UNREGISTER_HOST_CONTROLLER_RESPONSE;
        }

        @Override
        protected void sendResponse(final OutputStream outputStream) throws IOException {
            getController().removeClient(hostId);
        }
    }

    private class GetFileOperation extends RegistryOperation {
        private File localPath;

        @Override
        protected final byte getResponseCode() {
            return DomainControllerProtocol.GET_FILE_RESPONSE;
        }

        @Override
        protected void readRequest(final InputStream inputStream) throws IOException {
            final byte rootId;
            final String filePath;
            final FileRepository localFileRepository = getController().getFileRepository();
            ByteDataInput input = null;
            try {
                input = new SimpleByteDataInput(inputStream);
                expectHeader(input, DomainControllerProtocol.PARAM_ROOT_ID);
                rootId = input.readByte();
                expectHeader(input, DomainControllerProtocol.PARAM_FILE_PATH);
                filePath = input.readUTF();

                switch (rootId) {
                    case DomainControllerProtocol.PARAM_ROOT_ID_FILE: {
                        localPath = localFileRepository.getFile(filePath);
                        break;
                    }
                    case DomainControllerProtocol.PARAM_ROOT_ID_CONFIGURATION: {
                        localPath = localFileRepository.getConfigurationFile(filePath);
                        break;
                    }
                    case DomainControllerProtocol.PARAM_ROOT_ID_DEPLOYMENT: {
                        byte[] hash = HashUtil.hexStringToByteArray(filePath);
                        localPath = localFileRepository.getDeploymentRoot(hash);
                        break;
                    }
                    default: {
                        throw new IOException(String.format("Invalid root id [%d]", rootId));
                    }
                }
            } finally {
                StreamUtils.safeClose(input);
            }
        }

        @Override
        protected void sendResponse(final OutputStream outputStream) throws IOException {
            ByteDataOutput output = null;
            try {
                output = new SimpleByteDataOutput(outputStream);
                output.writeByte(DomainControllerProtocol.PARAM_NUM_FILES);
                if (localPath == null || !localPath.exists()) {
                    output.writeInt(-1);
                } else if (localPath.isFile()) {
                    output.writeInt(1);
                    writeFile(localPath, output);
                } else {
                    final List<File> childFiles = getChildFiles(localPath);
                    output.writeInt(childFiles.size());
                    for (File child : childFiles) {
                        writeFile(child, output);
                    }
                }
                output.close();
            } finally {
                StreamUtils.safeClose(output);
            }
        }

        private List<File> getChildFiles(final File base) {
            final List<File> childFiles = new ArrayList<File>();
            getChildFiles(base, childFiles);
            return childFiles;
        }

        private void getChildFiles(final File base, final List<File> childFiles) {
            for (File child : base.listFiles()) {
                if (child.isFile()) {
                    childFiles.add(child);
                } else {
                    getChildFiles(child, childFiles);
                }
            }
        }

        private String getRelativePath(final File parent, final File child) {
            return child.getAbsolutePath().substring(parent.getAbsolutePath().length());
        }

        private void writeFile(final File file, final DataOutput output) throws IOException {
            output.writeByte(DomainControllerProtocol.FILE_START);
            output.writeByte(DomainControllerProtocol.PARAM_FILE_PATH);
            output.writeUTF(getRelativePath(localPath, file));
            output.writeByte(DomainControllerProtocol.PARAM_FILE_SIZE);
            output.writeLong(file.length());
            InputStream inputStream = null;
            try {
                inputStream = new FileInputStream(file);
                byte[] buffer = new byte[8192];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    output.write(buffer, 0, len);
                }
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException ignored) {
                    }
                }
            }
            output.writeByte(DomainControllerProtocol.FILE_END);
        }
    }

    private static class RemoteHostControllerClient implements HostControllerClient {
        final ProxyController remote;
        final Connection connection;
        final String hostId;
        final PathAddress proxyNodeAddress;
        final ManagementRequestConnectionStrategy connectionStrategy;
        final ThreadFactory threadFactory = Executors.defaultThreadFactory();
        final ExecutorService executorService = Executors.newCachedThreadPool(threadFactory);

        public RemoteHostControllerClient(String hostId, Connection connection) {
            this.hostId = hostId;
            this.connection = connection;
            this.proxyNodeAddress = PathAddress.pathAddress(PathElement.pathElement(HOST, getId()));
            this.remote = RemoteProxyController.create(connection, proxyNodeAddress);
            this.connectionStrategy = new ManagementRequestConnectionStrategy.ExistingConnectionStrategy(connection);
        }

        @Override
        public PathAddress getProxyNodeAddress() {
            return remote.getProxyNodeAddress();
        }

        @Override
        public OperationResult execute(ExecutionContext executionContext, ResultHandler handler) {
            return remote.execute(executionContext, handler);
        }

        @Override
        public ModelNode execute(ExecutionContext executionContext) throws CancellationException {
            return remote.execute(executionContext);
        }

        @Override
        public OperationResult execute(final ExecutionContext executionContext, final ResultHandler handler,
                final ControllerTransactionContext transaction) {

            if (executionContext == null) {
                throw new IllegalArgumentException("Null execution context");
            }
            if (handler == null) {
                throw new IllegalArgumentException("Null handler");
            }
            if (transaction == null) {
                throw new IllegalArgumentException("Null transaction");
            }

            final AsynchronousOperation result = new AsynchronousOperation();
            executorService.execute (new Runnable() {
                @Override
                public void run() {
                    try {
                        Future<Void> f = new ExecuteTransactionalRequest(result, executionContext, handler, transaction.getTransactionId()).execute(connectionStrategy);

                        while (true) {
                            try {
                                //Avoid this thread hanging forever if the client gets shut down
                                f.get(500, TimeUnit.MILLISECONDS);
                                break;
                            } catch (TimeoutException e) {
                                if (executorService.isShutdown()) {
                                    break;
                                }
                            }
                        }

                    } catch (Exception e) {
                        throw new RuntimeException("Failed to execute operation ", e);
                    }
                }
            });
            return new OperationResult() {
                @Override
                public Cancellable getCancellable() {
                    return result;
                }

                @Override
                public ModelNode getCompensatingOperation() {
                    return null;
                }
            };
        }

        @Override
        public String getId() {
            return hostId;
        }

        @Override
        public boolean isActive() {
            return connection.getPeerAddress() != null;
        }

        private ModelNode readNode(InputStream in) throws IOException {
            ModelNode node = new ModelNode();
            node.readExternal(in);
            return node;
        }

        private abstract class ModelControllerRequest<T> extends ManagementRequest<T>{
            @Override
            protected byte getHandlerId() {
                return HANDLER_ID;
            }
        }

        private class AsynchronousOperation implements Cancellable {
            SimpleFuture<Integer> asynchronousId = new SimpleFuture<Integer>();

            @Override
            public boolean cancel() {
                try {
                    int i = asynchronousId.get().intValue();
                    if (i >= 0) {
                        return new CancelAsynchronousOperationRequest(i).executeForResult(connectionStrategy);
                    }
                    else return false;
                } catch (Exception e) {
                    throw new RuntimeException("Could not cancel request ", e);
                }
            }

            void setAsynchronousId(int i) {
                asynchronousId.set(Integer.valueOf(i));
            }
        }

        private class ExecuteTransactionalRequest extends ModelControllerRequest<Void> {

            private final AsynchronousOperation result;
            private final ExecutionContext executionContext;
            private final ResultHandler handler;
            private final ModelNode transactionId;

            ExecuteTransactionalRequest(AsynchronousOperation result, ExecutionContext executionContext, ResultHandler handler, ModelNode transactionId) {
                this.result = result;
                this.executionContext = executionContext;
                this.handler = handler;
                this.transactionId = transactionId;
            }

            @Override
            protected byte getRequestCode() {
                return EXECUTE_TRANSACTIONAL_REQUEST;
            }

            @Override
            protected byte getResponseCode() {
                return EXECUTE_TRANSACTIONAL_RESPONSE;
            }

            /** {@inheritDoc} */
            @Override
            protected void sendRequest(int protocolVersion, OutputStream output) throws IOException {
                output.write(TRANSACTION_ID);
                transactionId.writeExternal(output);
                output.write(ModelControllerClientProtocol.PARAM_OPERATION);
                executionContext.getOperation().writeExternal(output);

                List<InputStream> streams = executionContext.getInputStreams();
                for (InputStream in : streams) {
                    output.write(ModelControllerClientProtocol.PARAM_INPUT_STREAM);
                    //Just copy the stream contents for now - remoting will handle this better
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    try {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            bout.write(buffer, 0, read);
                        }
                    } finally {
                        StreamUtils.safeClose(in);
                    }

                    byte[] bytes = bout.toByteArray();
                    StreamUtils.writeInt(output, bytes.length);
                    try {
                        for (byte b : bytes) {
                            output.write(b);
                        }
                    } finally {
                        StreamUtils.safeClose(in);
                    }
                }
                output.write(ModelControllerClientProtocol.PARAM_REQUEST_END);
            }


            /** {@inheritDoc} */
            @Override
            protected Void receiveResponse(InputStream input) throws IOException {
                try {
                    LOOP:
                    while (true) {
                        int command = input.read();
                        switch (command) {
                            case ModelControllerClientProtocol.PARAM_HANDLE_RESULT_FRAGMENT:{
                                expectHeader(input, ModelControllerClientProtocol.PARAM_LOCATION);
                                int length = StreamUtils.readInt(input);
                                String[] location = new String[length];
                                for (int i = 0 ; i < length ; i++) {
                                    location[i] = StreamUtils.readUTFZBytes(input);
                                }
                                expectHeader(input, ModelControllerClientProtocol.PARAM_OPERATION);
                                ModelNode node = readNode(input);
                                handler.handleResultFragment(location, node);
                                break;
                            }
                            case ModelControllerClientProtocol.PARAM_HANDLE_CANCELLATION:{
                                handler.handleCancellation();
                                break LOOP;
                            }
                            case ModelControllerClientProtocol.PARAM_HANDLE_RESULT_FAILED:{
                                expectHeader(input, ModelControllerClientProtocol.PARAM_OPERATION);
                                ModelNode node = readNode(input);
                                // FIXME need some sort of translation
                                handler.handleFailed(node);
                                break LOOP;
                            }
                            case ModelControllerClientProtocol.PARAM_HANDLE_RESULT_COMPLETE:{
                                expectHeader(input, ModelControllerClientProtocol.PARAM_OPERATION);
                                ModelNode node = readNode(input); // TODO: Where does this go
                                handler.handleResultComplete();
                                break LOOP;
                            }
                            case ModelControllerClientProtocol.PARAM_REQUEST_ID:{
                                result.setAsynchronousId(StreamUtils.readInt(input));
                                break;
                            }
                            default:{
                                throw new IllegalStateException("Unknown response code " + command);
                            }
                        }
                    }
                } catch (Exception e) {
                    handler.handleFailed(new ModelNode().set(e.toString()));
                }
                return null;
            }
        }

        private class CancelAsynchronousOperationRequest extends ModelControllerRequest<Boolean> {

            private final int asynchronousId;

            CancelAsynchronousOperationRequest(int asynchronousId) {
                this.asynchronousId = asynchronousId;
            }

            @Override
            protected byte getRequestCode() {
                return ModelControllerClientProtocol.CANCEL_ASYNCHRONOUS_OPERATION_REQUEST;
            }

            @Override
            protected byte getResponseCode() {
                return ModelControllerClientProtocol.CANCEL_ASYNCHRONOUS_OPERATION_RESPONSE;
            }

            /** {@inheritDoc} */
            @Override
            protected void sendRequest(int protocolVersion, OutputStream output) throws IOException {
                output.write(ModelControllerClientProtocol.PARAM_REQUEST_ID);
                StreamUtils.writeInt(output, asynchronousId);
            }


            /** {@inheritDoc} */
            @Override
            protected Boolean receiveResponse(InputStream input) throws IOException {
                return StreamUtils.readBoolean(input);
            }
        }

    }

    private static class SimpleFuture<V> implements Future<V> {

        private V value;
        private volatile boolean done;
        private final Lock lock = new ReentrantLock();
        private final Condition hasValue = lock.newCondition();

        /**
         * Always returns <code>false</code>
         *
         * @return <code>false</code>
         */
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {

            lock.lock();
            try {
                while (!done) {
                    hasValue.await();
                }
                return value;
            }
            finally {
                lock.unlock();
            }
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {

            long deadline = unit.toMillis(timeout) + System.currentTimeMillis();
            lock.lock();
            try {
                while (!done) {
                    long remaining = deadline - System.currentTimeMillis();
                    if (remaining <= 0) {
                        throw new TimeoutException();
                    }
                    hasValue.await(remaining, TimeUnit.MILLISECONDS);
                }
                return value;
            }
            finally {
                lock.unlock();
            }
        }

        /**
         * Always returns <code>false</code>
         *
         * @return <code>false</code>
         */
        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return done;
        }

        public void set(V value) {
            lock.lock();
            try {
                this.value = value;
                done = true;
                hasValue.signalAll();
            }
            finally {
                lock.unlock();
            }
        }
    }
}
