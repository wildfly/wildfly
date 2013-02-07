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
package org.jboss.as.configadmin.parser;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.configadmin.ConfigAdmin;
import org.jboss.as.configadmin.service.ConfigAdminInternal;
import org.jboss.as.configadmin.service.ConfigAdminServiceImpl;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Domain extension used to initialize the ConfigAdmin subsystem.
 *
 * @author Thomas.Diesler@jboss.com
 * @author David Bosschaert
 */
public class ConfigAdminExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "configadmin";
    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 1;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

    private static final String RESOURCE_NAME = ConfigAdminExtension.class.getPackage().getName() + ".LocalDescriptions";

    static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, ConfigAdminExtension.class.getClassLoader(), true, true);
    }


    static ConfigAdminInternal getConfigAdminService(OperationContext context) {
        ServiceController<?> controller = context.getServiceRegistry(true).getService(ConfigAdmin.SERVICE_NAME);
        return controller != null ? (ConfigAdminServiceImpl) controller.getValue() : null;
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.VERSION_1_0.getUriString(), ConfigAdminParser.INSTANCE);
    }

    @Override
    public void initialize(ExtensionContext context) {

        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);
        subsystem.registerSubsystemModel(new ConfigAdminRootResource());

        if (context.isRegisterTransformers()) {
            registerTransformers_1_0_0(subsystem);
        }

        subsystem.registerXMLElementWriter(ConfigAdminParser.INSTANCE);
    }

    private void registerTransformers_1_0_0(final SubsystemRegistration subsystemRegistration) {
        final ModelVersion version = ModelVersion.create(1, 0, 0);

        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        builder.addChildResource(PathElement.pathElement(ModelConstants.CONFIGURATION))
            .getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, ConfigurationResource.ENTRIES)
                .end()
            .addOperationTransformationOverride(ModelConstants.UPDATE)
                .setCustomOperationTransformer(new OperationTransformer() {
                    @Override
                    public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation)
                            throws OperationFailedException {
                        ModelNode remove = operation.clone();
                        remove.get(OP).set(REMOVE);
                        remove.remove(ModelConstants.ENTRIES);

                        ModelNode add = operation.clone();
                        add.get(OP).set(ADD);

                        ModelNode composite = new ModelNode();
                        composite.get(OP).set(COMPOSITE);
                        composite.get(OP_ADDR).setEmptyList();
                        composite.get(STEPS).add(remove);
                        composite.get(STEPS).add(add);

                        return new TransformedOperation(composite, OperationResultTransformer.ORIGINAL_RESULT);
                    }
                });
        TransformationDescription.Tools.register(builder.build(), subsystemRegistration, version);
    }
}
