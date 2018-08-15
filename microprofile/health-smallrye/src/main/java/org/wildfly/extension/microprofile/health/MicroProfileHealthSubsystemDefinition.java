/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.health;

import java.util.Arrays;
import java.util.Collection;

import io.smallrye.health.SmallRyeHealthReporter;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
public class MicroProfileHealthSubsystemDefinition extends PersistentResourceDefinition {

    static final String HEALTH_REPORTER_CAPABILITY = "org.wildlfy.microprofile.health.reporter";
    static final RuntimeCapability<Void> HEALTH_REPORTER_RUNTIME_CAPABILITY =
            RuntimeCapability.Builder.of(HEALTH_REPORTER_CAPABILITY, SmallRyeHealthReporter.class)
                    .build();

    public static final ServiceName HEALTH_REPORTER_SERVICE = ServiceName.parse(HEALTH_REPORTER_CAPABILITY);

    static final String HTTP_EXTENSIBILITY_CAPABILITY = "org.wildfly.management.http.extensible";
    static final RuntimeCapability<Void> EXTENSION_CAPABILITY = RuntimeCapability.Builder.of(MicroProfileHealthExtension.EXTENSION_NAME)
            .addRequirements(HTTP_EXTENSIBILITY_CAPABILITY)
            .build();

    static final AttributeDefinition SECURITY_ENABLED = SimpleAttributeDefinitionBuilder.create("security-enabled", ModelType.BOOLEAN)
            .setDefaultValue(new ModelNode(true))
            .setRequired(false)
            .setRestartAllServices()
            .setAllowExpression(true)
            .build();
    static final AttributeDefinition[] ATTRIBUTES = { SECURITY_ENABLED };

    protected MicroProfileHealthSubsystemDefinition() {
        super(new Parameters(MicroProfileHealthExtension.SUBSYSTEM_PATH,
                MicroProfileHealthExtension.getResourceDescriptionResolver(MicroProfileHealthExtension.SUBSYSTEM_NAME))
                .setAddHandler(new MicroProfileHealthSubsystemAdd())
                .setRemoveHandler(new MicroProfileHealthSubsystemRemove())
                .setCapabilities(HEALTH_REPORTER_RUNTIME_CAPABILITY, EXTENSION_CAPABILITY));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(ATTRIBUTES);
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);

        CheckOperation.register(resourceRegistration);
    }


}
