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
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformersSubRegistration;
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
    private static final int MANAGEMENT_API_MINOR_VERSION = 0;
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
        //There is no difference in the model between 1.1.0 and 1.0.0 but 1.0.0 does not like "start-level"=>undefined, so we remove this here.
        ModelVersion version = ModelVersion.create(1, 0, 0);
        TransformersSubRegistration subsystemTransformer = subsystem.registerModelTransformers(version, ResourceTransformer.DEFAULT);
        TransformersSubRegistration capability = subsystemTransformer.registerSubResource(PathElement.pathElement(ModelConstants.CAPABILITY), new ResourceTransformer() {
            @Override
            public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource)
                    throws OperationFailedException {
                ModelNode model = resource.getModel();
                removeUndefinedStartLevel(model);
                ResourceTransformationContext childContext = context.addTransformedResource(PathAddress.EMPTY_ADDRESS, resource);
                childContext.processChildren(resource);

            }
        });
        capability.registerOperationTransformer(ModelDescriptionConstants.ADD, new OperationTransformer() {

            @Override
            public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation)
                    throws OperationFailedException {
                ModelNode op = operation.clone();
                removeUndefinedStartLevel(op);
                return new TransformedOperation(op, OperationResultTransformer.ORIGINAL_RESULT);
            }
        });
    }

    private void removeUndefinedStartLevel(ModelNode model) {
        if (model.has(ModelConstants.STARTLEVEL) && !model.hasDefined(ModelConstants.STARTLEVEL)) {
            model.remove(ModelConstants.STARTLEVEL);
        }
    }
}
