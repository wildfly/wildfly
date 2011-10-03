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
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.ejb3.subsystem.deployment.EntityBeanResourceDefinition;
import org.jboss.as.ejb3.subsystem.deployment.MessageDrivenBeanResourceDefinition;
import org.jboss.as.ejb3.subsystem.deployment.SingletonBeanResourceDefinition;
import org.jboss.as.ejb3.subsystem.deployment.StatefulSessionBeanResourceDefinition;
import org.jboss.as.ejb3.subsystem.deployment.StatelessSessionBeanResourceDefinition;
import org.jboss.dmr.ModelNode;

import java.util.Locale;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;

/**
 * Extension that provides the EJB3 subsystem.
 *
 * @author Emanuel Muckenhuber
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class EJB3Extension implements Extension {

    public static final String SUBSYSTEM_NAME = "ejb3";
    public static final String NAMESPACE_1_0 = "urn:jboss:domain:ejb3:1.0";
    public static final String NAMESPACE_1_1 = "urn:jboss:domain:ejb3:1.1";
    public static final String NAMESPACE_1_2 = "urn:jboss:domain:ejb3:1.2";

    private static final String RESOURCE_NAME = EJB3Extension.class.getPackage().getName() + ".LocalDescriptions";

    public static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, EJB3Extension.class.getClassLoader(), true, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);

        subsystem.registerXMLElementWriter(EJB3Subsystem12Parser.INSTANCE);

        final ManagementResourceRegistration subsystemRegistration = subsystem.registerSubsystemModel(EJB3SubsystemRootResourceDefinition.INSTANCE);

        // describe operation for the subsystem
        subsystemRegistration.registerOperationHandler(DESCRIBE, SubsystemDescribeHandler.INSTANCE, SubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

        // subsystem=ejb3/strict-max-bean-instance-pool=*
        subsystemRegistration.registerSubModel(StrictMaxPoolResourceDefinition.INSTANCE);

        // subsystem=ejb3/timer-service=*
        subsystemRegistration.registerSubModel(TimerServiceResourceDefinition.INSTANCE);

        ResourceDefinition deploymentsDef = new SimpleResourceDefinition(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME),
                                                                         getResourceDescriptionResolver("deployed"));
        final ManagementResourceRegistration deploymentsRegistration = subsystem.registerDeploymentModel(deploymentsDef);
        deploymentsRegistration.registerSubModel(EntityBeanResourceDefinition.INSTANCE);
        deploymentsRegistration.registerSubModel(MessageDrivenBeanResourceDefinition.INSTANCE);
        deploymentsRegistration.registerSubModel(SingletonBeanResourceDefinition.INSTANCE);
        deploymentsRegistration.registerSubModel(StatelessSessionBeanResourceDefinition.INSTANCE);
        deploymentsRegistration.registerSubModel(StatefulSessionBeanResourceDefinition.INSTANCE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(NAMESPACE_1_0, EJB3Subsystem10Parser.INSTANCE);
        context.setSubsystemXmlMapping(NAMESPACE_1_1, EJB3Subsystem11Parser.INSTANCE);
        context.setSubsystemXmlMapping(NAMESPACE_1_2, EJB3Subsystem12Parser.INSTANCE);
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
