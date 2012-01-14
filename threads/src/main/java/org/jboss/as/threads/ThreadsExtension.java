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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.threads.CommonAttributes.BOUNDED_QUEUE_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.QUEUELESS_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.SCHEDULED_THREAD_POOL;
import static org.jboss.as.threads.CommonAttributes.THREADS;
import static org.jboss.as.threads.CommonAttributes.THREAD_FACTORY;
import static org.jboss.as.threads.CommonAttributes.UNBOUNDED_QUEUE_THREAD_POOL;
import static org.jboss.as.threads.ThreadsDescriptionUtil.addBoundedQueueThreadPool;
import static org.jboss.as.threads.ThreadsDescriptionUtil.addQueuelessThreadPool;
import static org.jboss.as.threads.ThreadsDescriptionUtil.addScheduledThreadPool;
import static org.jboss.as.threads.ThreadsDescriptionUtil.addThreadFactory;
import static org.jboss.as.threads.ThreadsDescriptionUtil.addUnboundedQueueThreadPool;
import static org.jboss.as.threads.ThreadsDescriptionUtil.pathAddress;

import java.util.Locale;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;

/**
 * Extension for thread management.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ThreadsExtension implements Extension {

    private static final Logger log = Logger.getLogger("org.jboss.as.threads");

    public static String SUBSYSTEM_NAME = "threads";

    static final String RESOURCE_NAME = ThreadsExtension.class.getPackage().getName() + ".LocalDescriptions";

    public static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix, boolean useUnprefixedChildTypes) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, ThreadsExtension.class.getClassLoader(), true, useUnprefixedChildTypes);
    }

    @Override
    public void initialize(final ExtensionContext context) {

        log.debugf("Initializing Threading Extension");

        final boolean registerRuntimeOnly = context.isRuntimeOnlyRegistrationValid();

        // Register the remoting subsystem
        final SubsystemRegistration registration = context.registerSubsystem(THREADS, 1, 0);
        registration.registerXMLElementWriter(ThreadsParser.INSTANCE);

        // Remoting subsystem description and operation handlers
        final ManagementResourceRegistration subsystem = registration.registerSubsystemModel(new ThreadSubsystemResourceDefinition(registerRuntimeOnly));
//        subsystem.registerOperationHandler(DESCRIBE, ThreadsSubsystemDescribeHandler.INSTANCE,
//                ThreadsSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
        subsystem.registerOperationHandler(DESCRIBE, GenericSubsystemDescribeHandler.INSTANCE,
                GenericSubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
    }

    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.CURRENT.getUriString(), ThreadsParser.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.THREADS_1_0.getUriString(), ThreadsParser.INSTANCE);
    }

    private static class ThreadsSubsystemDescribeHandler implements OperationStepHandler, DescriptionProvider {
        static final ThreadsSubsystemDescribeHandler INSTANCE = new ThreadsSubsystemDescribeHandler();

        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            ModelNode result = context.getResult();

            result.add(Util.getEmptyOperation(ADD, pathAddress(PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME))));

            final ModelNode model = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));

            addBoundedQueueThreadPools(result, model);
            addQueuelessThreadPools(result, model);
            addScheduledThreadPools(result, model);
            addThreadFactories(result, model);
            addUnboundedQueueThreadPools(result, model);

            context.completeStep();
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
