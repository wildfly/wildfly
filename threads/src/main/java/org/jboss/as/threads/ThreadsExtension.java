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
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.AttributeAccess.Storage;
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
        registration.registerXMLElementWriter(NewThreadsParser.INSTANCE);
        // Remoting subsystem description and operation handlers
        final ModelNodeRegistration subsystem = registration.registerSubsystemModel(SUBSYSTEM_PROVIDER);
        subsystem.registerOperationHandler(ADD, ThreadsSubsystemAdd.INSTANCE, ThreadsSubsystemAdd.INSTANCE, false);
        subsystem.registerOperationHandler(DESCRIBE, ThreadsSubsystemDescribeHandler.INSTANCE,
                ThreadsSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

        NewThreadsUtils.registerOperations(subsystem);
    }

    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), NewThreadsParser.INSTANCE);
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

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return CommonDescriptions.getSubsystemDescribeOperation(locale);
        }

        private void addBoundedQueueThreadPools(final ModelNode result, final ModelNode model) {
            if (model.hasDefined(BOUNDED_QUEUE_THREAD_POOL)) {
                ModelNode pools = model.get(BOUNDED_QUEUE_THREAD_POOL);
                for (Property poolProp : pools.asPropertyList()) {
                    final ModelNode operation = Util.getEmptyOperation(
                            ADD,
                            pathAddress(PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME),
                                    PathElement.pathElement(BOUNDED_QUEUE_THREAD_POOL, poolProp.getName())));
                    final ModelNode pool = poolProp.getValue();

                    operation.get(NAME).set(pool.require(NAME));
                    if (pool.hasDefined(THREAD_FACTORY)) {
                        operation.get(THREAD_FACTORY).set(pool.get(THREAD_FACTORY));
                    }
                    if (pool.hasDefined(PROPERTIES)) {
                        operation.get(PROPERTIES).set(pool.get(PROPERTIES));
                    }
                    if (pool.hasDefined(MAX_THREADS)) {
                        operation.get(MAX_THREADS).set(pool.get(MAX_THREADS));
                    }
                    if (pool.hasDefined(KEEPALIVE_TIME)) {
                        operation.get(KEEPALIVE_TIME).set(pool.get(KEEPALIVE_TIME));
                    }
                    if (pool.hasDefined(BLOCKING)) {
                        operation.get(BLOCKING).set(pool.get(BLOCKING));
                    }
                    if (pool.hasDefined(HANDOFF_EXECUTOR)) {
                        operation.get(HANDOFF_EXECUTOR).set(pool.get(HANDOFF_EXECUTOR));
                    }
                    if (pool.hasDefined(ALLOW_CORE_TIMEOUT)) {
                        operation.get(ALLOW_CORE_TIMEOUT).set(pool.get(ALLOW_CORE_TIMEOUT));
                    }
                    if (pool.hasDefined(QUEUE_LENGTH)) {
                        operation.get(QUEUE_LENGTH).set(pool.get(QUEUE_LENGTH));
                    }
                    if (pool.hasDefined(CORE_THREADS)) {
                        operation.get(CORE_THREADS).set(pool.get(CORE_THREADS));
                    }
                    result.add(operation);
                }
            }
        }

        private void addQueuelessThreadPools(final ModelNode result, final ModelNode model) {
            if (model.hasDefined(QUEUELESS_THREAD_POOL)) {
                ModelNode pools = model.get(QUEUELESS_THREAD_POOL);
                for (Property poolProp : pools.asPropertyList()) {
                    final ModelNode operation = Util.getEmptyOperation(
                            ADD,
                            pathAddress(PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME),
                                    PathElement.pathElement(QUEUELESS_THREAD_POOL, poolProp.getName())));
                    final ModelNode pool = poolProp.getValue();

                    operation.get(NAME).set(pool.require(NAME));
                    if (pool.hasDefined(THREAD_FACTORY)) {
                        operation.get(THREAD_FACTORY).set(pool.get(THREAD_FACTORY));
                    }
                    if (pool.hasDefined(PROPERTIES)) {
                        operation.get(PROPERTIES).set(pool.get(PROPERTIES));
                    }
                    if (pool.hasDefined(MAX_THREADS)) {
                        operation.get(MAX_THREADS).set(pool.get(MAX_THREADS));
                    }
                    if (pool.hasDefined(KEEPALIVE_TIME)) {
                        operation.get(KEEPALIVE_TIME).set(pool.get(KEEPALIVE_TIME));
                    }
                    if (pool.hasDefined(BLOCKING)) {
                        operation.get(BLOCKING).set(pool.get(BLOCKING));
                    }
                    if (pool.hasDefined(HANDOFF_EXECUTOR)) {
                        operation.get(HANDOFF_EXECUTOR).set(pool.get(HANDOFF_EXECUTOR));
                    }
                    result.add(operation);
                }
            }
        }

        private void addThreadFactories(final ModelNode result, final ModelNode model) {
            if (model.hasDefined(THREAD_FACTORY)) {
                ModelNode pools = model.get(THREAD_FACTORY);
                for (Property poolProp : pools.asPropertyList()) {
                    final ModelNode operation = Util.getEmptyOperation(
                            ADD,
                            pathAddress(PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME),
                                    PathElement.pathElement(THREAD_FACTORY, poolProp.getName())));
                    final ModelNode pool = poolProp.getValue();

                    operation.get(NAME).set(pool.require(NAME));
                    if (pool.hasDefined(GROUP_NAME)) {
                        operation.get(GROUP_NAME).set(pool.get(GROUP_NAME));
                    }
                    if (pool.hasDefined(THREAD_NAME_PATTERN)) {
                        operation.get(THREAD_NAME_PATTERN).set(pool.get(THREAD_NAME_PATTERN));
                    }
                    if (pool.hasDefined(PRIORITY)) {
                        operation.get(PRIORITY).set(pool.get(PRIORITY));
                    }
                    if (pool.hasDefined(PROPERTIES)) {
                        operation.get(PROPERTIES).set(pool.get(PROPERTIES));
                    }
                    result.add(operation);
                }
            }
        }

        private void addScheduledThreadPools(final ModelNode result, final ModelNode model) {
            if (model.hasDefined(SCHEDULED_THREAD_POOL)) {
                ModelNode pools = model.get(SCHEDULED_THREAD_POOL);
                for (Property poolProp : pools.asPropertyList()) {
                    final ModelNode operation = Util.getEmptyOperation(
                            ADD,
                            pathAddress(PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME),
                                    PathElement.pathElement(SCHEDULED_THREAD_POOL, poolProp.getName())));
                    final ModelNode pool = poolProp.getValue();

                    operation.get(NAME).set(pool.require(NAME));
                    if (pool.hasDefined(THREAD_FACTORY)) {
                        operation.get(THREAD_FACTORY).set(pool.get(THREAD_FACTORY));
                    }
                    if (pool.hasDefined(PROPERTIES)) {
                        operation.get(PROPERTIES).set(pool.get(PROPERTIES));
                    }
                    if (pool.hasDefined(MAX_THREADS)) {
                        operation.get(MAX_THREADS).set(pool.get(MAX_THREADS));
                    }
                    if (pool.hasDefined(KEEPALIVE_TIME)) {
                        operation.get(KEEPALIVE_TIME).set(pool.get(KEEPALIVE_TIME));
                    }
                    result.add(operation);
                }
            }
        }

        private void addUnboundedQueueThreadPools(final ModelNode result, final ModelNode model) {
            if (model.hasDefined(UNBOUNDED_QUEUE_THREAD_POOL)) {
                ModelNode pools = model.get(UNBOUNDED_QUEUE_THREAD_POOL);
                for (Property poolProp : pools.asPropertyList()) {
                    final ModelNode operation = Util.getEmptyOperation(
                            ADD,
                            pathAddress(PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME),
                                    PathElement.pathElement(UNBOUNDED_QUEUE_THREAD_POOL, poolProp.getName())));
                    final ModelNode pool = poolProp.getValue();

                    operation.get(NAME).set(pool.require(NAME));
                    if (pool.hasDefined(THREAD_FACTORY)) {
                        operation.get(THREAD_FACTORY).set(pool.get(THREAD_FACTORY));
                    }
                    if (pool.hasDefined(PROPERTIES)) {
                        operation.get(PROPERTIES).set(pool.get(PROPERTIES));
                    }
                    if (pool.hasDefined(MAX_THREADS)) {
                        operation.get(MAX_THREADS).set(pool.get(MAX_THREADS));
                    }
                    if (pool.hasDefined(KEEPALIVE_TIME)) {
                        operation.get(KEEPALIVE_TIME).set(pool.get(KEEPALIVE_TIME));
                    }
                    result.add(operation);
                }
            }
        }

        private ModelNode pathAddress(PathElement... elements) {
            return PathAddress.pathAddress(elements).toModelNode();
        }
    }

}
