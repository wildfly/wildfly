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

package org.jboss.as.ee.subsystem;

import java.util.Locale;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * JBossAS domain extension used to initialize the ee subsystem handlers and associated classes.
 *
 * @author Weston M. Price
 * @author Emanuel Muckenhuber
 */
public class EeExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "ee";
    private static final String RESOURCE_NAME = EeExtension.class.getPackage().getName() + ".LocalDescriptions";

    static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, EeExtension.class.getClassLoader(), true, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, 1, 0);

        // Register the root subsystem resource.
        final ManagementResourceRegistration rootResource = subsystem.registerSubsystemModel(EeSubsystemRootResource.INSTANCE);

        // Mandatory describe operation
        rootResource.registerOperationHandler(DESCRIBE, EESubsystemDescribeHandler.INSTANCE, EESubsystemDescribeHandler.INSTANCE, false, OperationEntry.EntryType.PRIVATE);

        subsystem.registerXMLElementWriter(EESubsystemParser11.INSTANCE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.EE_1_0.getUriString(), EESubsystemParser10.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.EE_1_1.getUriString(), EESubsystemParser11.INSTANCE);
    }

    static ModelNode createEESubSystemAddOperation() {
        final ModelNode subsystem = new ModelNode();
        subsystem.get(OP).set(ADD);
        subsystem.get(OP_ADDR).add(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);
        return subsystem;
    }


    private static class EESubsystemDescribeHandler implements OperationStepHandler, DescriptionProvider {
        static final EESubsystemDescribeHandler INSTANCE = new EESubsystemDescribeHandler();

        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
            final ModelNode model = Resource.Tools.readModel(resource);
            final ModelNode op = createEESubSystemAddOperation();
            if (model.hasDefined(GlobalModulesDefinition.GLOBAL_MODULES)) {
                op.get(GlobalModulesDefinition.GLOBAL_MODULES).set(model.get(GlobalModulesDefinition.GLOBAL_MODULES));
            }
            if (model.hasDefined(Element.EAR_SUBDEPLOYMENTS_ISOLATED.getLocalName())) {
                op.get(Element.EAR_SUBDEPLOYMENTS_ISOLATED.getLocalName()).set(model.get(Element.EAR_SUBDEPLOYMENTS_ISOLATED.getLocalName()));
            }
            if (model.hasDefined(Element.SPEC_DESCRIPTOR_PROPERTY_REPLACEMENT.getLocalName())) {
                op.get(Element.SPEC_DESCRIPTOR_PROPERTY_REPLACEMENT.getLocalName()).set(model.get(Element.SPEC_DESCRIPTOR_PROPERTY_REPLACEMENT.getLocalName()));
            }
            context.getResult().add(op);
            context.completeStep();
        }

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return CommonDescriptions.getSubsystemDescribeOperation(locale);
        }
    }

}
