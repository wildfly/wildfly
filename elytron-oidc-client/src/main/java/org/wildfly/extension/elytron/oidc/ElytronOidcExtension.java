/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.elytron.oidc;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * An {@link Extension} to add support for OpenID Connect.
 *
 * @author <a href="mailto:fjuma@redhat.com">Farah Juma</a>
 */
public class ElytronOidcExtension implements Extension {

    /**
     * The name of our subsystem within the model.
     */
    public static final String SUBSYSTEM_NAME = "elytron-oidc-client";

    protected static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);

    private static final String RESOURCE_NAME = ElytronOidcExtension.class.getPackage().getName() + ".LocalDescriptions";

    protected static final ModelVersion VERSION_1_0_0 = ModelVersion.create(1, 0, 0);
    protected static final ModelVersion VERSION_2_0_0 = ModelVersion.create(2, 0, 0);
    private static final ModelVersion CURRENT_MODEL_VERSION = VERSION_2_0_0;

    private static final ElytronOidcSubsystemParser_1_0 ELYTRON_OIDC_SUBSYSTEM_PARSER_1_0 = new ElytronOidcSubsystemParser_1_0();
    private static final ElytronOidcSubsystemParser_2_0 CURRENT_PARSER = new ElytronOidcSubsystemParser_2_0();

    static ResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefixes) {
        StringBuilder sb = new StringBuilder(SUBSYSTEM_NAME);
        if (keyPrefixes != null) {
            for (String current : keyPrefixes) {
                sb.append(".").append(current);
            }
        }
        return new StandardResourceDescriptionResolver(sb.toString(), RESOURCE_NAME, ElytronOidcExtension.class.getClassLoader(),
                true, false);
    }
    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        subsystem.registerXMLElementWriter(CURRENT_PARSER);

        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(new ElytronOidcSubsystemDefinition());
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
    }

    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, ElytronOidcSubsystemParser_1_0.NAMESPACE_1_0, ELYTRON_OIDC_SUBSYSTEM_PARSER_1_0);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, ElytronOidcSubsystemParser_2_0.NAMESPACE_2_0, CURRENT_PARSER);
    }


}