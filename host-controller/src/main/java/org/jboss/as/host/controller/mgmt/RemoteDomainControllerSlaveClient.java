package org.jboss.as.host.controller.mgmt;

import static org.jboss.as.protocol.old.ProtocolUtils.expectHeader;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
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
import org.jboss.as.protocol.ProtocolChannel;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementClientChannelStrategy;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.old.StreamUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.threads.JBossThreadFactory;

/**
 * Client used by Host Controller to talk to the servers. The servers connect to HC.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
class RemoteDomainControllerSlaveClient implements DomainControllerSlaveClient {

    private static final Logger log = Logger.getLogger("org.jboss.as.domain.controller");

    // We delegate non-transactional requests
    private final ModelController delegate;
    private final String hostId;
    private final ProtocolChannel channel;
    private final ThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("RemoteDomainConnection-threads"), Boolean.FALSE, null, "%G - %t", null, null, AccessController.getContext());
    private final ExecutorService executorService = Executors.newCachedThreadPool(threadFactory);

    public RemoteDomainControllerSlaveClient(final String hostId, final ProtocolChannel channel) {
        this.hostId = hostId;
        this.channel = channel;
        this.delegate = new ModelControllerClientToModelControllerAdapter(channel, executorService);
    }

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
            return new ExecuteSynchronousTransactionalRequest(operation, transaction).executeForResult(executorService, getConnectionStrategy());
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
                    Future<ModelNode> f = new ExecuteAsyncTransactionalRequest(result, operation, handler, transaction).execute(executorService, getConnectionStrategy());

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
        try {
            return new IsActiveRequest().executeForResult(executorService, getConnectionStrategy());
        } catch (Exception e) {
            return false;
        }
    }

    private ModelNode readNode(DataInput in) throws IOException {
        ModelNode node = new ModelNode();
        node.readExternal(in);
        return node;
    }

    private ManagementClientChannelStrategy getConnectionStrategy() {
        try {
            return ManagementClientChannelStrategy.create(channel);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private abstract class ModelControllerRequest<T> extends ManagementRequest<T>{

        @Override
        protected T readResponse(DataInput input) throws IOException {
            return null;
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
                    return new CancelAsynchronousOperationRequest(i).executeForResult(executorService, getConnectionStrategy());
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

        /** {@inheritDoc} */
        @Override
        protected ModelNode readResponse(DataInput input) throws IOException {
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

        /** {@inheritDoc} */
        @Override
        protected ModelNode readResponse(DataInput input) throws IOException {
            try {
                LOOP:
                while (true) {
                    byte command = input.readByte();
                    switch (command) {
                        case ModelControllerClientProtocol.PARAM_OPERATION : {
                            result.setCompensatingOperation(readNode(input));
                            break;
                        }
                        case ModelControllerClientProtocol.PARAM_HANDLE_RESULT_FRAGMENT:{
                            expectHeader(input, ModelControllerClientProtocol.PARAM_LOCATION);
                            int length = input.readInt();
                            String[] location = new String[length];
                            for (int i = 0 ; i < length ; i++) {
                                location[i] = input.readUTF();
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
                            result.setAsynchronousId(input.readInt());
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
        protected void writeRequest(int protocolVersion, FlushableDataOutput output) throws IOException {
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
                output.writeInt(bytes.length);
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

        /** {@inheritDoc} */
        @Override
        protected void writeRequest(int protocolVersion, FlushableDataOutput output) throws IOException {
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

        /** {@inheritDoc} */
        @Override
        protected void writeRequest(int protocolVersion, FlushableDataOutput output) throws IOException {
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
                new CommitTransactionRequest(transactionId).executeForResult(executorService, getConnectionStrategy());
            } catch (Exception e) {
                log.errorf(e, "Failed committing management transaction %s on host %s", transactionId, hostId);
                // TODO flag the host as out of sync
            }
        }

        @Override
        public void rollback() {
            try {
                new RollbackTransactionRequest(transactionId).executeForResult(executorService, getConnectionStrategy());
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

        /** {@inheritDoc} */
        @Override
        protected void writeRequest(int protocolVersion, FlushableDataOutput output) throws IOException {
            output.write(ModelControllerClientProtocol.PARAM_REQUEST_ID);
            output.writeInt(asynchronousId);
        }


        /** {@inheritDoc} */
        @Override
        protected Boolean readResponse(DataInput input) throws IOException {
            return input.readBoolean();
        }
    }

    private class IsActiveRequest extends ModelControllerRequest<Boolean> {
        @Override
        public final byte getRequestCode() {
            return DomainControllerProtocol.IS_ACTIVE_REQUEST;
        }


        @Override
        protected Boolean readResponse(final DataInput input) throws IOException {
            return true;  // If we made it here, we correctly established a connection
        }
    }

}
