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
package org.jboss.as.threads;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import org.jboss.as.controller.registry.AttributeAccess;
import static org.jboss.as.threads.CommonAttributes.ALLOW_CORE_TIMEOUT;
import static org.jboss.as.threads.CommonAttributes.BLOCKING;
import static org.jboss.as.threads.CommonAttributes.BOUNDED_QUEUE_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.CORE_THREADS;
import static org.jboss.as.threads.CommonAttributes.GROUP_NAME;
import static org.jboss.as.threads.CommonAttributes.HANDOFF_EXECUTOR;
import static org.jboss.as.threads.CommonAttributes.KEEPALIVE_TIME;
import static org.jboss.as.threads.CommonAttributes.MAX_THREADS;
import static org.jboss.as.threads.CommonAttributes.NAME;
import static org.jboss.as.threads.CommonAttributes.PRIORITY;
import static org.jboss.as.threads.CommonAttributes.PROPERTIES;
import static org.jboss.as.threads.CommonAttributes.QUEUELESS_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.QUEUE_LENGTH;
import static org.jboss.as.threads.CommonAttributes.SCHEDULED_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.THREADS;
import static org.jboss.as.threads.CommonAttributes.THREAD_FACTORY;
import static org.jboss.as.threads.CommonAttributes.THREAD_NAME_PATTERN;
import static org.jboss.as.threads.CommonAttributes.UNBOUNDED_QUEUE_THREAD_POOL;
import static org.jboss.as.threads.ThreadsDescriptionUtil.addBoundedQueueThreadPool;
import static org.jboss.as.threads.ThreadsDescriptionUtil.addQueuelessThreadPool;
import static org.jboss.as.threads.ThreadsDescriptionUtil.addScheduledThreadPool;
import static org.jboss.as.threads.ThreadsDescriptionUtil.addThreadFactory;
import static org.jboss.as.threads.ThreadsDescriptionUtil.addUnboundedQueueThreadPool;
import static org.jboss.as.threads.ThreadsDescriptionUtil.pathAddress;
import static org.jboss.as.threads.ThreadsSubsystemProviders.BOUNDED_QUEUE_THREAD_POOL_DESC;
import static org.jboss.as.threads.ThreadsSubsystemProviders.QUEUELESS_THREAD_POOL_DESC;
import static org.jboss.as.threads.ThreadsSubsystemProviders.SCHEDULED_THREAD_POOL_DESC;
import static org.jboss.as.threads.ThreadsSubsystemProviders.SUBSYSTEM_PROVIDER;
import static org.jboss.as.threads.ThreadsSubsystemProviders.THREAD_FACTORY_DESC;
import static org.jboss.as.threads.ThreadsSubsystemProviders.UNBOUNDED_QUEUE_THREAD_POOL_DESC;

import java.util.Locale;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;

/**
 * Extension for thread management.
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ThreadsExtension implements Extension {
    private static final Logger log = Logger.getLogger("org.jboss.as.threads");

    private static String SUBSYSTEM_NAME = "threads";

    @Override
    public void initialize(final ExtensionContext context) {

        log.debugf("Initializing Threading Extension");

        // Register the remoting subsystem
        final SubsystemRegistration registration = context.registerSubsystem(THREADS);
        registration.registerXMLElementWriter(ThreadsParser.INSTANCE);
        // Remoting subsystem description and operation handlers
        final ModelNodeRegistration subsystem = registration.registerSubsystemModel(SUBSYSTEM_PROVIDER);
        subsystem.registerOperationHandler(ADD, ThreadsSubsystemAdd.INSTANCE, ThreadsSubsystemAdd.INSTANCE, false);
        subsystem.registerOperationHandler(DESCRIBE, ThreadsSubsystemDescribeHandler.INSTANCE,
                ThreadsSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

        final ModelNodeRegistration threadFactories = subsystem.registerSubModel(PathElement.pathElement(THREAD_FACTORY),
                THREAD_FACTORY_DESC);
        threadFactories.registerOperationHandler(ADD, ThreadFactoryAdd.INSTANCE, ThreadFactoryAdd.INSTANCE, false);
        threadFactories.registerOperationHandler(REMOVE, ThreadFactoryRemove.INSTANCE, ThreadFactoryRemove.INSTANCE, false);
        threadFactories.registerReadWriteAttribute(THREAD_NAME_PATTERN, null, ThreadFactoryThreadNamePatternUpdate.INSTANCE,
                AttributeAccess.Storage.CONFIGURATION);
        threadFactories.registerReadWriteAttribute(GROUP_NAME, null, ThreadFactoryGroupNameUpdate.INSTANCE,
                AttributeAccess.Storage.CONFIGURATION);
        threadFactories.registerReadWriteAttribute(PRIORITY, null, ThreadFactoryPriorityUpdate.INSTANCE, AttributeAccess.Storage.CONFIGURATION);

        final ModelNodeRegistration boundedQueueThreadPools = subsystem.registerSubModel(
                PathElement.pathElement(BOUNDED_QUEUE_THREAD_POOL), BOUNDED_QUEUE_THREAD_POOL_DESC);
        boundedQueueThreadPools.registerOperationHandler(ADD, BoundedQueueThreadPoolAdd.INSTANCE,
                BoundedQueueThreadPoolAdd.INSTANCE, false);
        boundedQueueThreadPools.registerOperationHandler(REMOVE, BoundedQueueThreadPoolRemove.INSTANCE,
                BoundedQueueThreadPoolRemove.INSTANCE, false);

        final ModelNodeRegistration unboundedQueueThreadPools = subsystem.registerSubModel(
                PathElement.pathElement(UNBOUNDED_QUEUE_THREAD_POOL), UNBOUNDED_QUEUE_THREAD_POOL_DESC);
        unboundedQueueThreadPools.registerOperationHandler(ADD, UnboundedQueueThreadPoolAdd.INSTANCE,
                UnboundedQueueThreadPoolAdd.INSTANCE, false);
        unboundedQueueThreadPools.registerOperationHandler(REMOVE, UnboundedQueueThreadPoolRemove.INSTANCE,
                UnboundedQueueThreadPoolRemove.INSTANCE, false);

        final ModelNodeRegistration queuelessThreadPools = subsystem.registerSubModel(
                PathElement.pathElement(QUEUELESS_THREAD_POOL), QUEUELESS_THREAD_POOL_DESC);
        queuelessThreadPools.registerOperationHandler(ADD, QueuelessThreadPoolAdd.INSTANCE, QueuelessThreadPoolAdd.INSTANCE,
                false);
        queuelessThreadPools.registerOperationHandler(REMOVE, QueuelessThreadPoolRemove.INSTANCE,
                QueuelessThreadPoolRemove.INSTANCE, false);

        final ModelNodeRegistration scheduledThreadPools = subsystem.registerSubModel(
                PathElement.pathElement(SCHEDULED_THREAD_POOL), SCHEDULED_THREAD_POOL_DESC);
        scheduledThreadPools.registerOperationHandler(ADD, ScheduledThreadPoolAdd.INSTANCE, ScheduledThreadPoolAdd.INSTANCE,
                false);
        scheduledThreadPools.registerOperationHandler(REMOVE, ScheduledThreadPoolRemove.INSTANCE,
                ScheduledThreadPoolRemove.INSTANCE, false);
    }

    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), ThreadsParser.INSTANCE);
    }

    static class ThreadsSubsystemDescribeHandler implements ModelQueryOperationHandler, DescriptionProvider {
        static final ThreadsSubsystemDescribeHandler INSTANCE = new ThreadsSubsystemDescribeHandler();

        @Override
        public OperationResult execute(final OperationContext context, final ModelNode operation,
                final ResultHandler resultHandler) {
            ModelNode result = new ModelNode();

            result.add(Util.getEmptyOperation(ADD, pathAddress(PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME))));

            final ModelNode model = context.getSubModel();
            addBoundedQueueThreadPools(result, model);
            addQueuelessThreadPools(result, model);
            addScheduledThreadPools(result, model);
            addThreadFactories(result, model);
            addUnboundedQueueThreadPools(result, model);

            resultHandler.handleResultFragment(Util.NO_LOCATION, result);
            resultHandler.handleResultComplete();
            return new BasicOperationResult();
        }

        private void addBoundedQueueThreadPools(final ModelNode result, final ModelNode model) {
            if (model.hasDefined(BOUNDED_QUEUE_THREAD_POOL)) {
                ModelNode pools = model.get(BOUNDED_QUEUE_THREAD_POOL);
                for (Property poolProp : pools.asPropertyList()) {
                    addBoundedQueueThreadPool(result, poolProp.getValue(), PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME), PathElement.pathElement(BOUNDED_QUEUE_THREAD_POOL, poolProp.getName()));
                }
            }
        }

        private void addQueuelessThreadPools(final ModelNode result, final ModelNode model) {
            if (model.hasDefined(QUEUELESS_THREAD_POOL)) {
                ModelNode pools = model.get(QUEUELESS_THREAD_POOL);
                for (Property poolProp : pools.asPropertyList()) {
                    addQueuelessThreadPool(result, poolProp.getValue(), PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME), PathElement.pathElement(QUEUELESS_THREAD_POOL, poolProp.getName()));
                }
            }
        }

        private void addThreadFactories(final ModelNode result, final ModelNode model) {
            if (model.hasDefined(THREAD_FACTORY)) {
                ModelNode pools = model.get(THREAD_FACTORY);
                for (Property poolProp : pools.asPropertyList()) {
                    addThreadFactory(result, poolProp.getValue(), PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME), PathElement.pathElement(THREAD_FACTORY, poolProp.getName()));
                }
            }
        }

        private void addScheduledThreadPools(final ModelNode result, final ModelNode model) {
            if (model.hasDefined(SCHEDULED_THREAD_POOL)) {
                ModelNode pools = model.get(SCHEDULED_THREAD_POOL);
                for (Property poolProp : pools.asPropertyList()) {
                    addScheduledThreadPool(result, poolProp.getValue(), PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME), PathElement.pathElement(SCHEDULED_THREAD_POOL, poolProp.getName()));
                }
            }
        }

        private void addUnboundedQueueThreadPools(final ModelNode result, final ModelNode model) {
            if (model.hasDefined(UNBOUNDED_QUEUE_THREAD_POOL)) {
                ModelNode pools = model.get(UNBOUNDED_QUEUE_THREAD_POOL);
                for (Property poolProp : pools.asPropertyList()) {
                    addUnboundedQueueThreadPool(result, poolProp.getValue(), PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME), PathElement.pathElement(UNBOUNDED_QUEUE_THREAD_POOL, poolProp.getName()));
                }
            }
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return CommonDescriptions.getSubsystemDescribeOperation(locale);
        }
    }

}
