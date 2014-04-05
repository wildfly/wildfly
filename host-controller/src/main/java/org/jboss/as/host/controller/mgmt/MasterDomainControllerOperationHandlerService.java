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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;

import static java.security.AccessController.doPrivileged;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.controller.CurrentOperationIdHolder;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ProxyController.ProxyOperationControl;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.remote.AbstractModelControllerOperationHandlerFactoryService;
import org.jboss.as.controller.remote.ModelControllerClientOperationHandler;
import org.jboss.as.controller.remote.ModelControllerClientOperationHandlerFactoryService;
import org.jboss.as.controller.remote.TransactionalProtocolOperationHandler;
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.operations.PullDownDataForServerConfigOnSlaveHandler;
import org.jboss.as.domain.controller.operations.coordination.DomainControllerLockIdUtils;
import org.jboss.as.host.controller.HostControllerMessages;
import org.jboss.as.protocol.mgmt.ManagementChannelAssociation;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.protocol.mgmt.ManagementClientChannelStrategy;
import org.jboss.as.protocol.mgmt.ManagementPongRequestHandler;
import org.jboss.as.protocol.mgmt.ManagementRequestContext;
import org.wildfly.security.manager.action.GetAccessControlContextAction;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.remoting3.Channel;
import org.jboss.threads.JBossThreadFactory;

/**
 * Installs {@link MasterDomainControllerOperationHandlerImpl} which handles requests from slave DC to master DC.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MasterDomainControllerOperationHandlerService extends AbstractModelControllerOperationHandlerFactoryService {

    public static final ServiceName SERVICE_NAME = DomainController.SERVICE_NAME.append(ModelControllerClientOperationHandlerFactoryService.OPERATION_HANDLER_NAME_SUFFIX);

    private final DomainController domainController;
    private final HostControllerRegistrationHandler.OperationExecutor operationExecutor;
    private final TransactionalOperationExecutor txOperationExecutor;
    private final ManagementPongRequestHandler pongRequestHandler = new ManagementPongRequestHandler();
    private final ThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("slave-request-threads"), Boolean.FALSE, null, "%G - %t", null, null, doPrivileged(GetAccessControlContextAction.getInstance()));
    private volatile ExecutorService slaveRequestExecutor;
    private final DomainControllerRuntimeIgnoreTransformationRegistry runtimeIgnoreTransformationRegistry;

    public MasterDomainControllerOperationHandlerService(final DomainController domainController, final HostControllerRegistrationHandler.OperationExecutor operationExecutor, TransactionalOperationExecutor txOperationExecutor, DomainControllerRuntimeIgnoreTransformationRegistry runtimeIgnoreTransformationRegistry) {
        this.domainController = domainController;
        this.operationExecutor = operationExecutor;
        this.txOperationExecutor = txOperationExecutor;
        this.runtimeIgnoreTransformationRegistry = runtimeIgnoreTransformationRegistry;
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        pongRequestHandler.resetConnectionId();
        slaveRequestExecutor = new ThreadPoolExecutor(1, Integer.MAX_VALUE,
                5L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>(),
                threadFactory);
        super.start(context);
    }

    @Override
    public synchronized void stop(StopContext context) {
        slaveRequestExecutor.shutdown();
        super.stop(context);
    }

    @Override
    public ManagementChannelHandler startReceiving(final Channel channel) {
        final ManagementChannelHandler handler = new ManagementChannelHandler(ManagementClientChannelStrategy.create(channel), getExecutor());
        // Assemble the request handlers for the domain channel
        handler.addHandlerFactory(new HostControllerRegistrationHandler(handler, domainController, operationExecutor, slaveRequestExecutor, runtimeIgnoreTransformationRegistry));
        handler.addHandlerFactory(new ModelControllerClientOperationHandler(getController(), handler));
        handler.addHandlerFactory(new MasterDomainControllerOperationHandlerImpl(domainController, slaveRequestExecutor));
        handler.addHandlerFactory(pongRequestHandler);
        handler.addHandlerFactory(new DomainTransactionalProtocolOperationHandler(txOperationExecutor, handler));
        channel.receiveMessage(handler.getReceiver());
        return handler;
    }

    private class DomainTransactionalProtocolOperationHandler extends TransactionalProtocolOperationHandler {
        private final TransactionalOperationExecutor executor;

        private volatile SlaveRequest activeSlaveRequest;

        public DomainTransactionalProtocolOperationHandler(TransactionalOperationExecutor executor, ManagementChannelAssociation channelAssociation) {
            super(null, channelAssociation);
            this.executor = executor;
        }

        @Override
        protected ModelNode internalExecute(final ModelNode operation, final ManagementRequestContext<?> context, final OperationMessageHandler messageHandler, final ProxyOperationControl control,
                OperationAttachments attachments) {

            final OperationStepHandler handler;
            final String operationName = operation.require(OP).asString();
            if (operationName.equals(PullDownDataForServerConfigOnSlaveHandler.OPERATION_NAME)) {
                handler = new PullDownDataForServerConfigOnSlaveHandler(
                        SlaveChannelAttachments.getHostName(context.getChannel()),
                        SlaveChannelAttachments.getTransformers(context.getChannel()),
                        runtimeIgnoreTransformationRegistry);
            } else {
                throw HostControllerMessages.MESSAGES.cannotExecuteTransactionalOperationFromSlave(operationName);
            }

            Integer domainControllerLockId;
            if (operation.get(OPERATION_HEADERS).hasDefined(DomainControllerLockIdUtils.DOMAIN_CONTROLLER_LOCK_ID)) {
                domainControllerLockId = operation.get(OPERATION_HEADERS, DomainControllerLockIdUtils.DOMAIN_CONTROLLER_LOCK_ID).asInt();
            } else {
                domainControllerLockId = null;
            }

            final Integer slaveLockId = operation.get(OPERATION_HEADERS, DomainControllerLockIdUtils.SLAVE_CONTROLLER_LOCK_ID).asInt();
            if (domainControllerLockId == null) {
                synchronized (this) {
                    SlaveRequest slaveRequest = this.activeSlaveRequest;
                    if (slaveRequest != null) {
                        domainControllerLockId = slaveRequest.domainId;
                        slaveRequest.refCount.incrementAndGet();
                    }
                }
                //TODO https://issues.jboss.org/browse/AS7-6809 If there are many slaves calling back many of these threads will be blocked, and I
                //believe they are a finite resource
            }

            try {
                if (domainControllerLockId != null) {
                    return executor.joinActiveOperation(operation, messageHandler, control, attachments, handler, domainControllerLockId);
                } else {
                    ModelNode result = executor.executeAndAttemptLock(operation, messageHandler, control, attachments, new OperationStepHandler() {
                        @Override
                        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                            //Grab the lock id and store it
                            Integer domainControllerLockId = CurrentOperationIdHolder.getCurrentOperationID();
                            synchronized (this) {
                                activeSlaveRequest = new SlaveRequest(domainControllerLockId);
                            }
                            context.addStep(operation, handler, OperationContext.Stage.MODEL);
                            context.stepCompleted();
                        }
                    });
                    return result;
                }
            } finally {
                synchronized (this) {
                    SlaveRequest slaveRequest = this.activeSlaveRequest;
                    if (slaveRequest != null) {
                        int refcount = slaveRequest.refCount.decrementAndGet();
                        if (refcount == 0) {
                            activeSlaveRequest = null;
                        }
                    }
                }
            }
        }
    }

    public interface TransactionalOperationExecutor {
        ModelNode executeAndAttemptLock(ModelNode operation, OperationMessageHandler handler, ModelController.OperationTransactionControl control, OperationAttachments attachments, OperationStepHandler step);

        ModelNode joinActiveOperation(ModelNode operation, OperationMessageHandler handler, ModelController.OperationTransactionControl control, OperationAttachments attachments, OperationStepHandler step, int permit);
    }

    private final class SlaveRequest {
        private final int domainId;
        private final AtomicInteger refCount = new AtomicInteger(1);

        SlaveRequest(int domainId){
            this.domainId = domainId;
        }
    }
}
