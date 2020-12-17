/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.health;

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
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2020 Red Hat inc.
 */
public class HealthSubsystemDefinition extends PersistentResourceDefinition {

    static final String HTTP_EXTENSIBILITY_CAPABILITY = "org.wildfly.management.http.extensible";
    public static final String HEALTH_HTTP_SECURITY_CAPABILITY = "org.wildfly.extension.health.http-context.security-enabled";
    static final String CLIENT_FACTORY_CAPABILITY ="org.wildfly.management.model-controller-client-factory";
    static final String MANAGEMENT_EXECUTOR ="org.wildfly.management.executor";

    static final RuntimeCapability<Void> HEALTH_HTTP_CONTEXT_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.extension.health.http-context", HealthContextService.class)
            .addRequirements(HTTP_EXTENSIBILITY_CAPABILITY)
            .build();
    static final RuntimeCapability<Void> SERVER_HEALTH_PROBES_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.extension.health.server-probes", ServerProbesService.class)
            .addRequirements(CLIENT_FACTORY_CAPABILITY, MANAGEMENT_EXECUTOR)
            .build();


    static final AttributeDefinition SECURITY_ENABLED = SimpleAttributeDefinitionBuilder.create("security-enabled", ModelType.BOOLEAN)
            .setDefaultValue(ModelNode.TRUE)
            .setRequired(false)
            .setRestartAllServices()
            .setAllowExpression(true)
            .build();

    static final AttributeDefinition[] ATTRIBUTES = { SECURITY_ENABLED };

    protected HealthSubsystemDefinition() {
        super(new SimpleResourceDefinition.Parameters(HealthExtension.SUBSYSTEM_PATH,
                HealthExtension.getResourceDescriptionResolver(HealthExtension.SUBSYSTEM_NAME))
                .setAddHandler(HealthSubsystemAdd.INSTANCE)
                .setRemoveHandler(new ServiceRemoveStepHandler(HealthSubsystemAdd.INSTANCE))
                .addCapabilities(HEALTH_HTTP_CONTEXT_CAPABILITY, SERVER_HEALTH_PROBES_CAPABILITY));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

}