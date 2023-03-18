/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.microprofile.jwt.smallrye;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.EnumSet;
import java.util.List;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLDescriptionReader;
import org.jboss.as.controller.PersistentResourceXMLDescriptionWriter;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;


/**
 * A {@link Extension} to add support for MicroProfile JWT using SmallRye JWT.
 *
 * <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class MicroProfileJWTExtension implements Extension {

    /**
     * The name of our subsystem within the model.
     */
    public static final String SUBSYSTEM_NAME = "microprofile-jwt-smallrye";

    protected static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);

    static final ParentResourceDescriptionResolver SUBSYSTEM_RESOLVER = new SubsystemResourceDescriptionResolver(SUBSYSTEM_NAME, MicroProfileJWTExtension.class);

    protected static final ModelVersion VERSION_1_0_0 = ModelVersion.create(1, 0, 0);
    private static final ModelVersion CURRENT_MODEL_VERSION = VERSION_1_0_0;

    private final PersistentResourceXMLDescription currentDescription = MicroProfileJWTSubsystemSchema.CURRENT.getXMLDescription();

    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        subsystem.registerXMLElementWriter(new PersistentResourceXMLDescriptionWriter(this.currentDescription));

        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(new MicroProfileSubsystemDefinition());
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        for (MicroProfileJWTSubsystemSchema schema : EnumSet.allOf(MicroProfileJWTSubsystemSchema.class)) {
            XMLElementReader<List<ModelNode>> reader = (schema == MicroProfileJWTSubsystemSchema.CURRENT) ? new PersistentResourceXMLDescriptionReader(this.currentDescription) : schema;
            context.setSubsystemXmlMapping(SUBSYSTEM_NAME, schema.getNamespace().getUri(), reader);
        }
    }

}
