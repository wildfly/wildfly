/*
 * JBoss, Home of Professional Open Source. Copyright 2019, Red Hat, Inc., and
 * individual contributors as indicated by the @author tags. See the
 * copyright.txt file in the distribution for a full listing of individual
 * contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.wildfly.extension.microprofile.openapi;

import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Michael Edgar
 */
public class MicroProfileOpenAPISubsystemDefinition extends PersistentResourceDefinition {

    static final String CAPABILITY_NAME_MP_OAI_HTTP_CONTEXT = "org.wildfly.extension.microprofile.openapi.http-context";

    // Dependencies
    static final String CAPABILITY_UNDERTOW_SERVER = "org.wildfly.undertow.host";
    static final String CAPABILITY_NAME_MP_CONFIG = "org.wildlfy.microprofile.config";

    public static final RuntimeCapability<Void> HTTP_CONTEXT_CAPABILITY = RuntimeCapability.Builder
                .of(CAPABILITY_NAME_MP_OAI_HTTP_CONTEXT, OpenAPIContextService.class)
                .addRequirements(CAPABILITY_NAME_MP_CONFIG)
                .build();

    static final AttributeDefinition SERVER = SimpleAttributeDefinitionBuilder
                .create("server", ModelType.STRING)
                .setDefaultValue(new ModelNode("default-server"))
                .setRequired(true)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

    static final AttributeDefinition VIRTUAL_HOST = SimpleAttributeDefinitionBuilder
                .create("virtual-host", ModelType.STRING)
                .setDefaultValue(new ModelNode("default-host"))
                .setRequired(true)
                .setAllowExpression(true)
                .setRestartAllServices()
                .build();

    static final AttributeDefinition[] ATTRIBUTES = { SERVER, VIRTUAL_HOST };

    protected MicroProfileOpenAPISubsystemDefinition() {
        super(new SimpleResourceDefinition.Parameters(MicroProfileOpenAPIExtension.SUBSYSTEM_PATH,
                                                      MicroProfileOpenAPIExtension.getResourceDescriptionResolver(MicroProfileOpenAPIExtension.SUBSYSTEM_NAME))
                                          .setAddHandler(MicroProfileOpenAPISubsystemAdd.INSTANCE)
                                          .setRemoveHandler(new ServiceRemoveStepHandler(MicroProfileOpenAPISubsystemAdd.INSTANCE))
                                          .setCapabilities(HTTP_CONTEXT_CAPABILITY));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }
}
