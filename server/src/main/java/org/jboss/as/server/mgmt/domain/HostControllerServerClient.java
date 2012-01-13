/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.server.mgmt.domain;

import java.io.DataInput;
import java.io.File;
import java.io.IOException;
import java.security.AccessController;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.ControllerLogger;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.remote.TransactionalModelControllerOperationHandler;
import org.jboss.as.protocol.mgmt.AbstractManagementRequest;
import org.jboss.as.protocol.mgmt.AbstractMessageHandler;
import org.jboss.as.protocol.mgmt.ActiveOperation;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementChannelReceiver;
import org.jboss.as.protocol.mgmt.ManagementMessageHandler;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementRequestContext;
import org.jboss.as.server.ServerLogger;
import org.jboss.as.server.ServerMessages;
import org.jboss.as.server.file.repository.impl.RemoteFileRequestAndHandler.CannotCreateLocalDirectoryException;
import org.jboss.as.server.file.repository.impl.RemoteFileRequestAndHandler.DidNotReadEntireFileException;
import org.jboss.as.server.mgmt.domain.RemoteFileRepository.RemoteFileRepositoryExecutor;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.threads.AsyncFuture;
import org.jboss.threads.JBossThreadFactory;

/**
 * Client used to interact with the local HostController.
 * The HC counterpart is ServerToHostOperationHandler
 *
 * @author John Bailey
 * @author Emanuel Muckenhuber
 * @author Kabir Khan
 */
public class HostControllerServerClient implements Service<HostControllerServerClient> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("host", "controller", "client");

    private final InjectedValue<Channel> hcChannel = new InjectedValue<Channel>();
    private final InjectedValue<ModelController> controller = new InjectedValue<ModelController>();
    private final InjectedValue<RemoteFileRepository> remoteFileRepositoryValue = new InjectedValue<RemoteFileRepository>();

    private final String serverName;
    private final String serverProcessName;
    private final ThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("host-controller-connection-threads"), Boolean.FALSE, null, "%G - %t", null, null, AccessController.getContext());
    private final ExecutorService executor = Executors.newCachedThreadPool(threadFactory);
    private volatile HostControllerServerHandler registerHandler;
    //private volatile AbstractMessageHandler<File, Void> getFileHandler;

    public HostControllerServerClient(final String serverName, final String serverProcessName) {
        this.serverName = serverName;
        this.serverProcessName = serverProcessName;
    }

    /** {@inheritDoc} */
    public synchronized void start(final StartContext context) throws StartException {
        final Channel channel = hcChannel.getValue();
        final HostControllerServerHandler handler = new HostControllerServerHandler(controller.getValue(), executor);
        channel.addCloseHandler(new CloseHandler<Channel>() {
            @Override
            public void handleClose(final Channel closed, final IOException exception) {
                handler.shutdownNow();
            }
        });
        // Notify MSC asynchronously when the server gets registered
        context.asynchronous();
        try {
            handler.executeRegistrationRequest(channel, new ServerRegisterRequest(), context);
        } catch (Exception e) {
            throw ServerMessages.MESSAGES.failedToConnectToHC(e);
        }
        this.registerHandler = handler;
        remoteFileRepositoryValue.getValue().setRemoteFileRepositoryExecutor(new RemoteFileRepositoryExecutorImpl());

        channel.receiveMessage(ManagementChannelReceiver.createDelegating(handler));
    }

    /** {@inheritDoc} */
    public synchronized void stop(StopContext context) {
        final ManagementMessageHandler handler = this.registerHandler;
        if(handler != null) {
            handler.shutdown();
            try {
                if(! handler.awaitCompletion(100, TimeUnit.MILLISECONDS)) {
                    ControllerLogger.ROOT_LOGGER.debugf("HostController server client did not complete shutdown within timeout");
                }
            } catch (Exception e) {
                ControllerLogger.ROOT_LOGGER.warnf(e , "service shutdown did not complete");
            } finally {
                handler.shutdownNow();
            }
        }
    }

    public String getServerName(){
        return serverName;
    }

    public String getServerProcessName() {
        return serverProcessName;
    }

    /** {@inheritDoc} */
    public synchronized HostControllerServerClient getValue() throws IllegalStateException {
        return this;
    }

    public Injector<Channel> getHcChannelInjector() {
        return hcChannel;
    }

    public Injector<ModelController> getServerControllerInjector() {
        return controller;
    }

    public Injector<RemoteFileRepository> getRemoteFileRepositoryInjector() {
        return remoteFileRepositoryValue;
    }

    private class HostControllerServerHandler extends TransactionalModelControllerOperationHandler {

        private HostControllerServerHandler(final ModelController controller, final ExecutorService executorService) {
            super(controller, executorService);
        }

        protected AsyncFuture<Void> executeRegistrationRequest(final Channel channel, final ManagementRequest request, final StartContext callback) {
            final ActiveOperation support = super.registerActiveOperation(null, new ActiveOperation.CompletedCallback<Void>() {
                @Override
                public void completed(Void result) {
                    callback.complete();
                }

                @Override
                public void failed(Exception e) {
                    callback.failed(ServerMessages.MESSAGES.failedToConnectToHC(e));
                }

                @Override
                public void cancelled() {
                    callback.failed(ServerMessages.MESSAGES.cancelledHCConnect());
                }
            });
            return super.executeRequest(request, channel, support);
        }

        protected AsyncFuture executeGetFileRequest(final Channel channel, final ManagementRequest request){
            final ActiveOperation support = super.registerActiveOperation(null);
            return super.executeRequest(request, channel, support);
        }
    }

    private class ServerRegisterRequest extends AbstractManagementRequest<Void, Void> {

        @Override
        public byte getOperationType() {
            return DomainServerProtocol.REGISTER_REQUEST;
        }

        @Override
        protected void sendRequest(ActiveOperation.ResultHandler<Void> resultHandler, ManagementRequestContext<Void> voidManagementRequestContext, FlushableDataOutput output) throws IOException {
            output.write(DomainServerProtocol.PARAM_SERVER_NAME);
            output.writeUTF(serverProcessName);
        }

        @Override
        public void handleRequest(DataInput input, ActiveOperation.ResultHandler<Void> resultHandler, ManagementRequestContext<Void> voidManagementRequestContext) throws IOException {
            resultHandler.done(null);
        }

    }

    private class GetFileRequest extends AbstractManagementRequest<File, Void> {
        private final String hash;
        private final File localDeploymentFolder;

        private GetFileRequest(final String hash, final File localDeploymentFolder) {
            this.hash = hash;
            this.localDeploymentFolder = localDeploymentFolder;
        }

        @Override
        public byte getOperationType() {
            return DomainServerProtocol.GET_FILE_REQUEST;
        }

        @Override
        protected void sendRequest(ActiveOperation.ResultHandler<File> resultHandler, ManagementRequestContext<Void> context, FlushableDataOutput output) throws IOException {
            //The root id does not matter here
            ServerToHostRemoteFileRequestAndHandler.INSTANCE.sendRequest(output, (byte)0, hash);
        }

        @Override
        public void handleRequest(DataInput input, ActiveOperation.ResultHandler<File> resultHandler, ManagementRequestContext<Void> context) throws IOException {
            boolean error;
            try {
                File first = new File(localDeploymentFolder, hash.substring(0,2));
                File localPath = new File(first, hash.substring(2));
                ServerToHostRemoteFileRequestAndHandler.INSTANCE.handleResponse(input, localPath, ServerLogger.ROOT_LOGGER, resultHandler, context);
                resultHandler.done(null);
            } catch (CannotCreateLocalDirectoryException e) {
                resultHandler.failed(ServerMessages.MESSAGES.cannotCreateLocalDirectory(e.getDir()));
            } catch (DidNotReadEntireFileException e) {
                resultHandler.failed(ServerMessages.MESSAGES.didNotReadEntireFile(e.getMissing()));
            }
        }
    }

    private class RemoteFileRepositoryExecutorImpl implements RemoteFileRepositoryExecutor {
        public File getFile(final String relativePath, final byte repoId, final File localDeploymentFolder) {
            try {
                return (File)registerHandler.executeGetFileRequest(hcChannel.getValue(), new GetFileRequest(relativePath, localDeploymentFolder)).get();
            } catch (Exception e) {
                throw ServerMessages.MESSAGES.failedToGetFileFromRemoteRepository(e);
            }
        }
    }

    private static class GetFileOperationHandler extends AbstractMessageHandler<File, Void>{

        protected GetFileOperationHandler(ExecutorService executorService) {
            super(executorService);
        }

    }
}
