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

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

import java.util.Locale;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.*;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * @author Emanuel Muckenhuber
 */
public class EJB3Extension implements Extension {

    public static final String SUBSYSTEM_NAME = "ejb3";
    public static final String NAMESPACE_1_0 = "urn:jboss:domain:ejb3:1.0";
    public static final String NAMESPACE_1_1 = "urn:jboss:domain:ejb3:1.1";

    private static final EJB3Subsystem10Parser ejb3Subsystem10Parser = new EJB3Subsystem10Parser();
    private static final EJB3Subsystem11Parser ejb3Subsystem11Parser = new EJB3Subsystem11Parser();

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ManagementResourceRegistration subsystemRegistration = subsystem.registerSubsystemModel(EJB3SubsystemProviders.SUBSYSTEM);
        subsystem.registerXMLElementWriter(ejb3Subsystem11Parser);

        // register the operations
        // EJB3 subsystem ADD operation
        subsystemRegistration.registerOperationHandler(ADD, EJB3SubsystemAdd.INSTANCE, EJB3SubsystemAdd.INSTANCE, false);
        // describe operation for the subsystem
        subsystemRegistration.registerOperationHandler(DESCRIBE, SubsystemDescribeHandler.INSTANCE, SubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);
        // default slsb pool
        subsystemRegistration.registerReadWriteAttribute(DEFAULT_SLSB_INSTANCE_POOL, null, SetDefaultSLSBPool.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        // default MDB pool
        subsystemRegistration.registerReadWriteAttribute(DEFAULT_MDB_INSTANCE_POOL, null, SetDefaultMDBPool.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        // default resource adapter name
        subsystemRegistration.registerReadWriteAttribute(DEFAULT_RESOURCE_ADAPTER_NAME, null, SetDefaultResourceAdapterName.INSTANCE, AttributeAccess.Storage.CONFIGURATION);

        // subsystem=ejb3/strict-max-bean-instance-pool=*
        final ManagementResourceRegistration strictMaxPoolRegistration = subsystemRegistration.registerSubModel(
                PathElement.pathElement(EJB3SubsystemModel.STRICT_MAX_BEAN_INSTANCE_POOL), EJB3SubsystemProviders.STRICT_MAX_BEAN_INSTANCE_POOL);
        // register ADD and REMOVE operations for strict-max-pool
        strictMaxPoolRegistration.registerOperationHandler(ADD, StrictMaxPoolAdd.INSTANCE, StrictMaxPoolAdd.INSTANCE, false);
        strictMaxPoolRegistration.registerOperationHandler(REMOVE, StrictMaxPoolRemove.INSTANCE, StrictMaxPoolRemove.INSTANCE, false);


        final ManagementResourceRegistration timerService = subsystemRegistration.registerSubModel(
                EJB3SubsystemModel.TIMER_SERVICE_PATH, EJB3SubsystemProviders.TIMER_SERVICE);

        // register ADD and REMOVE operations for timer-service
        timerService.registerOperationHandler(ADD, TimerServiceAdd.INSTANCE, TimerServiceAdd.INSTANCE, false);
        timerService.registerOperationHandler(REMOVE, TimerServiceRemove.INSTANCE, TimerServiceRemove.INSTANCE, false);

        timerService.registerReadWriteAttribute(EJB3SubsystemModel.PATH, null,  WriteAttributeHandlers.WriteAttributeOperationHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        timerService.registerReadWriteAttribute(EJB3SubsystemModel.RELATIVE_TO, null,  WriteAttributeHandlers.WriteAttributeOperationHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        timerService.registerReadWriteAttribute(EJB3SubsystemModel.CORE_THREADS, null,  WriteAttributeHandlers.WriteAttributeOperationHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);
        timerService.registerReadWriteAttribute(EJB3SubsystemModel.MAX_THREADS, null,  WriteAttributeHandlers.WriteAttributeOperationHandler.INSTANCE, AttributeAccess.Storage.CONFIGURATION);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(NAMESPACE_1_0, ejb3Subsystem10Parser);
        context.setSubsystemXmlMapping(NAMESPACE_1_1, ejb3Subsystem11Parser);
    }

    private static ModelNode createAddSubSystemOperation(final ModelNode model) {
        final ModelNode address = new ModelNode();
        address.add(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);
        return org.jboss.as.controller.operations.common.Util.getOperation(ADD, address, model);
    }

    private static ModelNode createAddStrictMaxPoolOperation(final String name, final ModelNode model) {
        final ModelNode address = new ModelNode();
        address.add(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);
        address.add(EJB3SubsystemModel.STRICT_MAX_BEAN_INSTANCE_POOL, name);
        return org.jboss.as.controller.operations.common.Util.getOperation(ADD, address, model);
    }

    private static ModelNode createTimerServiceOperation(final ModelNode model) {
        final ModelNode address = new ModelNode();
        address.add(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);
        address.add(EJB3SubsystemModel.SERVICE, EJB3SubsystemModel.TIMER_SERVICE);
        return org.jboss.as.controller.operations.common.Util.getOperation(ADD, address, model);
    }

    private static class SubsystemDescribeHandler implements OperationStepHandler, DescriptionProvider {
        static final SubsystemDescribeHandler INSTANCE = new SubsystemDescribeHandler();

        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final ModelNode result = context.getResult();
            final Resource root = context.readResource(PathAddress.EMPTY_ADDRESS);
            result.add(createAddSubSystemOperation(root.getModel()));
            for (Resource.ResourceEntry pool : root.getChildren(EJB3SubsystemModel.STRICT_MAX_BEAN_INSTANCE_POOL)) {
                result.add(createAddStrictMaxPoolOperation(pool.getName(), pool.getModel()));
            }
            final Resource timerService = root.getChild(EJB3SubsystemModel.TIMER_SERVICE_PATH);
            if (timerService != null) {
                result.add(createTimerServiceOperation(timerService.getModel()));
            }

            context.completeStep();
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return new ModelNode(); // internal operation
        }
    }
}
