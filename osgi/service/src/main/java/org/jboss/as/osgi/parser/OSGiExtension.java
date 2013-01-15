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
package org.jboss.as.osgi.parser;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.RejectExpressionValuesTransformer;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.as.controller.transform.chained.ChainedOperationTransformer;
import org.jboss.as.controller.transform.chained.ChainedResourceTransformationContext;
import org.jboss.as.controller.transform.chained.ChainedResourceTransformer;
import org.jboss.as.controller.transform.chained.ChainedResourceTransformerEntry;
import org.jboss.dmr.ModelNode;

/**
 * Domain extension used to initialize the OSGi subsystem.
 *
 * @author Thomas.Diesler@jboss.com
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author David Bosschaert
 */
public class OSGiExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "osgi";

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 1;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.VERSION_1_0.getUriString(), OSGiNamespace10Parser.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.VERSION_1_1.getUriString(), OSGiNamespace11Parser.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.VERSION_1_2.getUriString(), OSGiNamespace12Parser.INSTANCE);
    }

    @Override
    public void initialize(ExtensionContext context) {

        boolean registerRuntimeOnly = context.isRuntimeOnlyRegistrationValid();

        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);
        subsystem.registerSubsystemModel(new OSGiRootResource(registerRuntimeOnly));

        subsystem.registerXMLElementWriter(OSGiSubsystemWriter.INSTANCE);

        if (context.isRegisterTransformers()) {
            registerTransformers1_0_0(subsystem);
        }
    }

    private void registerTransformers1_0_0(SubsystemRegistration subsystem) {

        // Root resource
        RejectExpressionValuesTransformer rootReject = new RejectExpressionValuesTransformer(OSGiRootResource.ACTIVATION);
        TransformersSubRegistration subsystemTransformer = subsystem.registerModelTransformers(ModelVersion.create(1, 0, 0), rootReject);
        subsystemTransformer.registerOperationTransformer(ModelDescriptionConstants.ADD, rootReject);
        subsystemTransformer.registerOperationTransformer(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION, rootReject.getWriteAttributeTransformer());

        // Capabilities
        RemoveUndefinedTransformer undefinedTransformer = new RemoveUndefinedTransformer();
        RejectExpressionValuesTransformer capabilityReject = new RejectExpressionValuesTransformer(FrameworkCapabilityResource.STARTLEVEL);
        TransformersSubRegistration capability = subsystemTransformer.registerSubResource(FrameworkCapabilityResource.CAPABILITY_PATH,
                new ChainedResourceTransformer(undefinedTransformer, capabilityReject.getChainedTransformer()));
        capability.registerOperationTransformer(ModelDescriptionConstants.ADD,
                new ChainedOperationTransformer(undefinedTransformer, capabilityReject));
        // Attribute is read-only so we don't want to apply reject-expression transformation to write-attribute
        //capability.registerOperationTransformer(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION, capabilityReject.getWriteAttributeTransformer());

        // Properties
        RejectExpressionValuesTransformer valueReject = new RejectExpressionValuesTransformer(FrameworkPropertyResource.VALUE);
        TransformersSubRegistration property = subsystemTransformer.registerSubResource(FrameworkPropertyResource.PROPERTY_PATH,
                (ResourceTransformer) valueReject);
        property.registerOperationTransformer(ModelDescriptionConstants.ADD, valueReject);
        property.registerOperationTransformer(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION, valueReject.getWriteAttributeTransformer());
    }

    /** 1.0.0 does not like "start-level"=>undefined, so we remove this here */
    private static class RemoveUndefinedTransformer implements ChainedResourceTransformerEntry, OperationTransformer {

        @Override
        public void transformResource(ChainedResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
            ModelNode model = resource.getModel();
            removeUndefinedStartLevel(model);
        }

        @Override
        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
            ModelNode op = operation.clone();
            removeUndefinedStartLevel(op);
            return new TransformedOperation(op, OperationResultTransformer.ORIGINAL_RESULT);
        }

        private static void removeUndefinedStartLevel(ModelNode model) {
            if (model.has(ModelConstants.STARTLEVEL) && !model.hasDefined(ModelConstants.STARTLEVEL)) {
                model.remove(ModelConstants.STARTLEVEL);
            }
        }
    }
}
