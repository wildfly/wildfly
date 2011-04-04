package org.jboss.as.host.controller.mgmt;

import java.security.AccessController;
import static org.jboss.as.protocol.ProtocolUtils.expectHeader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ControllerResource;
import org.jboss.as.controller.ControllerTransactionContext;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.client.ModelControllerClientProtocol;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.remote.ModelControllerClientToModelControllerAdapter;
import org.jboss.as.controller.remote.TransactionalModelControllerOperationHandler;
import org.jboss.as.domain.controller.DomainControllerSlaveClient;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementRequestConnectionStrategy;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.threads.JBossThreadFactory;

class RemoteDomainControllerSlaveClient implements DomainControllerSlaveClient {

    private static final Logger log = Logger.getLogger("org.jboss.as.domain.controller");
    private static final int CONNECTION_TIMEOUT = 5000;

    // We delegate non-transactional requests
    private final ModelController delegate;
//    private final Connection connection;
    private final String hostId;
    private final InetAddress slaveAddress;
    private final int slavePort;
//    private final ManagementRequestConnectionStrategy connectionStrategy;
    private final ThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("RemoteDomainConnection-threads"), Boolean.FALSE, null, "%G - %t", null, null, AccessController.getContext());
    private final ExecutorService executorService = Executors.newCachedThreadPool(threadFactory);

    public RemoteDomainControllerSlaveClient(String hostId, InetAddress slaveAddress, int slavePort) {
        this.hostId = hostId;
        this.slaveAddress = slaveAddress;
        this.slavePort = slavePort;
//        this.connection = connection;
        this.delegate = new ModelControllerClientToModelControllerAdapter(slaveAddress, slavePort);
//        this.connectionStrategy = new ManagementRequestConnectionStrategy.ExistingConnectionStrategy(connection);
    }

//    public RemoteDomainControllerSlaveClient(String hostId, Connection connection) {
//        this.hostId = hostId;
//        this.connection = connection;
//        this.delegate = new ModelControllerClientToModelControllerAdapter(connection);
//        this.connectionStrategy = new ManagementRequestConnectionStrategy.ExistingConnectionStrategy(connection);
//    }

    @Override
    public OperationResult execute(Operation operation, ResultHandler handler) {
        return delegate.execute(operation, handler);
    }

    @Override
    public ModelNode execute(Operation operation) throws CancellationException {
        return delegate.execute(operation);
    }

    @Override
    public ModelNode execute(final Operation operation, ControllerTransactionContext transaction) throws CancellationException {
        if (operation == null) {
            throw new IllegalArgumentException("Null operation");
        }
        try {
            return new ExecuteSynchronousTransactionalRequest(operation, transaction).executeForResult(getConnectionStrategy());
        } catch (ExecutionException e) {
            if (e.getCause() instanceof CancellationException) {
                throw new CancellationException(e.getCause().getMessage());
            }
            throw new RuntimeException("Failed to execute operation ", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute operation ", e);
        }
    }

    @Override
    public OperationResult execute(final Operation operation, final ResultHandler handler,
            final ControllerTransactionContext transaction) {

        if (operation == null) {
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
                    Future<ModelNode> f = new ExecuteAsyncTransactionalRequest(result, operation, handler, transaction).execute(getConnectionStrategy());

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
                return result.getCompensatingOperation();
            }
        };
    }

    @Override
    public String getId() {
        return hostId;
    }

    @Override
    public boolean isActive() {
//        return connection.getPeerAddress() != null;
        try {
            return new IsActiveRequest().executeForResult(getConnectionStrategy());
        } catch (Exception e) {
            return false;
        }
    }

    private ModelNode readNode(InputStream in) throws IOException {
        ModelNode node = new ModelNode();
        node.readExternal(in);
        return node;
    }

    private ManagementRequestConnectionStrategy getConnectionStrategy() {
        return new ManagementRequestConnectionStrategy.EstablishConnectingStrategy(slaveAddress, slavePort,
                CONNECTION_TIMEOUT, executorService, threadFactory);
    }

    private abstract class ModelControllerRequest<T> extends ManagementRequest<T>{
        @Override
        protected byte getHandlerId() {
            return TransactionalModelControllerOperationHandler.HANDLER_ID;
        }
    }

    private class AsynchronousOperation implements Cancellable {

        private SimpleFuture<ModelNode> compensatingOperation = new SimpleFuture<ModelNode>();
        // GuardedBy compensatingOperation
        private boolean compensatingOpSet = false;
        private SimpleFuture<Integer> asynchronousId = new SimpleFuture<Integer>();

        @Override
        public boolean cancel() {
            setCompensatingOperation(new ModelNode());
            try {
                int i = asynchronousId.get().intValue();
                if (i >= 0) {
                    return new CancelAsynchronousOperationRequest(i).executeForResult(getConnectionStrategy());
                }
                else return false;
            } catch (Exception e) {
                throw new RuntimeException("Could not cancel request ", e);
            }
        }

        void setAsynchronousId(int i) {
            asynchronousId.set(Integer.valueOf(i));
        }

        void setCompensatingOperation(ModelNode compensatingOp) {
            synchronized (compensatingOperation) {
                if (!compensatingOpSet) {
                    compensatingOperation.set(compensatingOp);
                    compensatingOpSet = true;
                }
            }
        }

        ModelNode getCompensatingOperation() {
            try {
                return compensatingOperation.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Could not obtain compensating operation", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("Could not obtain compensating operation", e);
            }
        }
    }

    private class ExecuteSynchronousTransactionalRequest extends TransactionalRequest<ModelNode> {

        ExecuteSynchronousTransactionalRequest(final Operation operation, final ControllerTransactionContext transaction) {
            super(operation, transaction);
        }

        @Override
        protected byte getRequestCode() {
            return TransactionalModelControllerOperationHandler.EXECUTE_TRANSACTIONAL_SYNCHRONOUS_REQUEST;
        }

        @Override
        protected byte getResponseCode() {
            return TransactionalModelControllerOperationHandler.EXECUTE_TRANSACTIONAL_SYNCHRONOUS_RESPONSE;
        }

        /** {@inheritDoc} */
        @Override
        protected ModelNode receiveResponse(InputStream input) throws IOException {
            expectHeader(input, ModelControllerClientProtocol.PARAM_OPERATION);
            return readNode(input);
        }
    }

    private class ExecuteAsyncTransactionalRequest extends TransactionalRequest<ModelNode> {

        private final AsynchronousOperation result;
        private final ResultHandler handler;

        ExecuteAsyncTransactionalRequest(final AsynchronousOperation result, final Operation operation,
                final ResultHandler handler, final ControllerTransactionContext transaction) {
            super(operation, transaction);
            this.result = result;
            this.handler = handler;
        }

        @Override
        protected byte getRequestCode() {
            return TransactionalModelControllerOperationHandler.EXECUTE_TRANSACTIONAL_REQUEST;
        }

        @Override
        protected byte getResponseCode() {
            return TransactionalModelControllerOperationHandler.EXECUTE_TRANSACTIONAL_RESPONSE;
        }

        /** {@inheritDoc} */
        @Override
        protected ModelNode receiveResponse(InputStream input) throws IOException {
            try {
                LOOP:
                while (true) {
                    int command = input.read();
                    switch (command) {
                        case ModelControllerClientProtocol.PARAM_OPERATION : {
                            result.setCompensatingOperation(readNode(input));
                            break;
                        }
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
                            ModelNode node = readNode(input);
                            handler.handleFailed(node);
                            break LOOP;
                        }
                        case ModelControllerClientProtocol.PARAM_HANDLE_RESULT_COMPLETE:{
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

    private abstract class TransactionalRequest<T> extends ModelControllerRequest<T> {

        private final Operation operation;
        private final ModelNode transactionId;

        TransactionalRequest(final Operation operation, final ControllerTransactionContext transaction) {
            this.operation = operation;
            this.transactionId = transaction.getTransactionId();
            transaction.registerResource(new ControllerResourceProxy(transactionId));
        }

        /** {@inheritDoc} */
        @Override
        protected void sendRequest(int protocolVersion, OutputStream output) throws IOException {
            output.write(TransactionalModelControllerOperationHandler.TRANSACTION_ID);
            transactionId.writeExternal(output);
            output.write(ModelControllerClientProtocol.PARAM_OPERATION);
            operation.getOperation().writeExternal(output);

            List<InputStream> streams = operation.getInputStreams();
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
    }

    private class CommitTransactionRequest extends ModelControllerRequest<Boolean> {

        private final ModelNode transactionId;

        CommitTransactionRequest(final ModelNode transactionId) {
            this.transactionId = transactionId;
        }

        @Override
        protected byte getRequestCode() {
            return TransactionalModelControllerOperationHandler.TRANSACTION_COMMIT_REQUEST;
        }

        @Override
        protected byte getResponseCode() {
            return TransactionalModelControllerOperationHandler.TRANSACTION_COMMIT_RESPONSE;
        }

        /** {@inheritDoc} */
        @Override
        protected void sendRequest(int protocolVersion, OutputStream output) throws IOException {
            output.write(TransactionalModelControllerOperationHandler.TRANSACTION_ID);
            transactionId.writeExternal(output);
        }
    }

    private class RollbackTransactionRequest extends ModelControllerRequest<Boolean> {

        private final ModelNode transactionId;

        RollbackTransactionRequest(final ModelNode transactionId) {
            this.transactionId = transactionId;
        }

        @Override
        protected byte getRequestCode() {
            return TransactionalModelControllerOperationHandler.TRANSACTION_ROLLBACK_REQUEST;
        }

        @Override
        protected byte getResponseCode() {
            return TransactionalModelControllerOperationHandler.TRANSACTION_ROLLBACK_RESPONSE;
        }

        /** {@inheritDoc} */
        @Override
        protected void sendRequest(int protocolVersion, OutputStream output) throws IOException {
            output.write(TransactionalModelControllerOperationHandler.TRANSACTION_ID);
            transactionId.writeExternal(output);
        }
    }

    private class ControllerResourceProxy implements ControllerResource {

        private final ModelNode transactionId;

        ControllerResourceProxy(final ModelNode transactionId) {
            this.transactionId = transactionId;
        }

        @Override
        public void commit() {
            try {
                new CommitTransactionRequest(transactionId).executeForResult(getConnectionStrategy());
            } catch (Exception e) {
                log.errorf(e, "Failed committing management transaction %s on host %s", transactionId, hostId);
                // TODO flag the host as out of sync
            }
        }

        @Override
        public void rollback() {
            try {
                new RollbackTransactionRequest(transactionId).executeForResult(getConnectionStrategy());
            } catch (Exception e) {
                // Less serious as failing to roll back just means the tx is sitting around
                log.warnf(e, "Failed rolling back management transaction %s on host %s", transactionId, hostId);
            }
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

    private class IsActiveRequest extends ModelControllerRequest<Boolean> {
        @Override
        public final byte getRequestCode() {
            return DomainControllerProtocol.IS_ACTIVE_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return DomainControllerProtocol.IS_ACTIVE_RESPONSE;
        }

        @Override
        protected Boolean receiveResponse(final InputStream input) throws IOException {
            return true;  // If we made it here, we correctly established a connection
        }
    }

}
