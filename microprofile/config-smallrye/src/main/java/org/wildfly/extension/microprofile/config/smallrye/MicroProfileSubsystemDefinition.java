/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.config.smallrye;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import java.util.Collection;
import java.util.Collections;

import org.eclipse.microprofile.config.spi.ConfigSource;
import org.eclipse.microprofile.config.spi.ConfigSourceProvider;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author <a href="mailto:tcerar@redhat.com">Tomaz Cerar</a>
 */
class MicroProfileSubsystemDefinition extends PersistentResourceDefinition {
    static final String CONFIG_CAPABILITY_NAME = "org.wildfly.microprofile.config";

    static final RuntimeCapability<Void> CONFIG_CAPABILITY =
            RuntimeCapability.Builder.of(CONFIG_CAPABILITY_NAME)
                    .addRequirements(WELD_CAPABILITY_NAME)
                    .build();

    protected MicroProfileSubsystemDefinition(Iterable<ConfigSourceProvider> providers, Iterable<ConfigSource> sources) {
        super(new SimpleResourceDefinition.Parameters(MicroProfileConfigExtension.SUBSYSTEM_PATH, MicroProfileConfigExtension.getResourceDescriptionResolver(MicroProfileConfigExtension.SUBSYSTEM_NAME))
                .setAddHandler(new MicroProfileConfigSubsystemAdd(providers, sources))
                .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
                .setCapabilities(CONFIG_CAPABILITY)
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptySet();
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        //you can register aditional operations here
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        //you can register attributes here
    }
}
