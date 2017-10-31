/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.subsystems.jca;

import static org.jboss.as.connector.subsystems.jca.Constants.ELYTRON_ENABLED_NAME;
import static org.jboss.as.connector.subsystems.jca.Constants.ELYTRON_MANAGED_SECURITY;
import static org.jboss.as.connector.subsystems.jca.Constants.WORKMANAGER;
import static org.jboss.as.connector.subsystems.jca.Constants.WORKMANAGER_LONG_RUNNING;
import static org.jboss.as.connector.subsystems.jca.Constants.WORKMANAGER_SHORT_RUNNING;
import static org.jboss.as.controller.OperationContext.Stage.MODEL;

import java.util.Set;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.metadata.api.common.Security;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReadResourceNameOperationStepHandler;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.threads.BoundedQueueThreadPoolAdd;
import org.jboss.as.threads.BoundedQueueThreadPoolRemove;
import org.jboss.as.threads.BoundedQueueThreadPoolResourceDefinition;
import org.jboss.as.threads.CommonAttributes;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class JcaWorkManagerDefinition extends SimpleResourceDefinition {
    protected static final PathElement PATH_WORK_MANAGER = PathElement.pathElement(WORKMANAGER);
    private final boolean registerRuntimeOnly;

    private JcaWorkManagerDefinition(final boolean registerRuntimeOnly) {
        super(PATH_WORK_MANAGER,
                JcaExtension.getResourceDescriptionResolver(PATH_WORK_MANAGER.getKey()),
                WorkManagerAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }

    public static JcaWorkManagerDefinition createInstance(final boolean registerRuntimeOnly) {
        return new JcaWorkManagerDefinition(registerRuntimeOnly);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        resourceRegistration.registerReadOnlyAttribute(WmParameters.NAME.getAttribute(), ReadResourceNameOperationStepHandler.INSTANCE);
        resourceRegistration.registerReadOnlyAttribute(WmParameters.ELYTRON_ENABLED.getAttribute(), null);

    }

    @Override
        public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        registerSubModels(resourceRegistration, registerRuntimeOnly);
    }

    static void registerSubModels(ManagementResourceRegistration resourceRegistration, boolean runtimeOnly) {
        final BoundedQueueThreadPoolAdd shortRunningThreadPoolAdd = new BoundedQueueThreadPoolAdd(false,
                ThreadsServices.STANDARD_THREAD_FACTORY_RESOLVER, ThreadsServices.STANDARD_HANDOFF_EXECUTOR_RESOLVER,
                ThreadsServices.EXECUTOR.append(WORKMANAGER_SHORT_RUNNING)) {
            @Override
            protected void populateModel(final OperationContext context, final ModelNode operation, final Resource resource)
                    throws OperationFailedException {
                super.populateModel(context, operation, resource);
                context.addStep(new OperationStepHandler(){
                    public void execute(OperationContext oc, ModelNode op) throws OperationFailedException {
                        checkThreadPool(oc, op, WORKMANAGER_SHORT_RUNNING);
                   }
                }, MODEL);
            }
        };
        resourceRegistration.registerSubModel(
                new JCAThreadPoolResourceDefinition(false, runtimeOnly, WORKMANAGER_SHORT_RUNNING, ThreadsServices.EXECUTOR.append(WORKMANAGER_SHORT_RUNNING),
                        CommonAttributes.BOUNDED_QUEUE_THREAD_POOL, shortRunningThreadPoolAdd, ReloadRequiredRemoveStepHandler.INSTANCE));

        final BoundedQueueThreadPoolAdd longRunningThreadPoolAdd = new BoundedQueueThreadPoolAdd(false,
                ThreadsServices.STANDARD_THREAD_FACTORY_RESOLVER, ThreadsServices.STANDARD_HANDOFF_EXECUTOR_RESOLVER,
                ThreadsServices.EXECUTOR.append(WORKMANAGER_LONG_RUNNING)) {
            @Override
            protected void populateModel(final OperationContext context, final ModelNode operation, final Resource resource)
                    throws OperationFailedException {
                super.populateModel(context, operation, resource);
                context.addStep(new OperationStepHandler(){
                    public void execute(OperationContext oc, ModelNode op) throws OperationFailedException {
                        checkThreadPool(oc, op, WORKMANAGER_LONG_RUNNING);
                   }
                }, MODEL);
            }
        };
        resourceRegistration.registerSubModel(
                new JCAThreadPoolResourceDefinition(false, runtimeOnly, WORKMANAGER_LONG_RUNNING, ThreadsServices.EXECUTOR.append(WORKMANAGER_LONG_RUNNING),
                        CommonAttributes.BOUNDED_QUEUE_THREAD_POOL, longRunningThreadPoolAdd, new BoundedQueueThreadPoolRemove(longRunningThreadPoolAdd)));

    }

    private static class JCAThreadPoolResourceDefinition extends BoundedQueueThreadPoolResourceDefinition {
        @SuppressWarnings("deprecation")
        protected JCAThreadPoolResourceDefinition(boolean blocking, boolean registerRuntimeOnly,
                String type, ServiceName serviceNameBase, String resolverPrefix, OperationStepHandler addHandler,
                OperationStepHandler removeHandler) {
            super(blocking, registerRuntimeOnly, type, serviceNameBase, resolverPrefix, addHandler, removeHandler);
        }
    }

    private static void checkThreadPool(final OperationContext context, final ModelNode operation, final String type) throws OperationFailedException {
        PathAddress threadPoolPath = context.getCurrentAddress();
        PathAddress workManagerPath = threadPoolPath.getParent();
        Set<String> entrySet = context.readResourceFromRoot(workManagerPath, false).getChildrenNames(type);
        if (entrySet.size() > 0
                && !entrySet.iterator().next().equals(threadPoolPath.getLastElement().getValue())) {
            throw ConnectorLogger.ROOT_LOGGER.oneThreadPoolWorkManager(threadPoolPath.getLastElement().getValue(), type, workManagerPath.getLastElement().getValue());
        }
    }

    public enum WmParameters {
        NAME(SimpleAttributeDefinitionBuilder.create("name", ModelType.STRING)
                .setAllowExpression(false)
                .setRequired(true)
                .setMeasurementUnit(MeasurementUnit.NONE)
                .setRestartAllServices()
                .setXmlName("name")
                .build()),
        ELYTRON_ENABLED(new SimpleAttributeDefinitionBuilder(ELYTRON_ENABLED_NAME, ModelType.BOOLEAN, true)
                .setXmlName(Security.Tag.ELYTRON_ENABLED.getLocalName())
                .setAllowExpression(true)
                .setDefaultValue(new ModelNode(ELYTRON_MANAGED_SECURITY))
                .build());

        WmParameters(SimpleAttributeDefinition attribute) {
            this.attribute = attribute;
        }

        public SimpleAttributeDefinition getAttribute() {
            return attribute;
        }

        private SimpleAttributeDefinition attribute;
    }

}
