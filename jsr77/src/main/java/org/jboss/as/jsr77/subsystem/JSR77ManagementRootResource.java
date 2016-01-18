/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jsr77.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

import java.util.Collection;
import java.util.Collections;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class JSR77ManagementRootResource extends PersistentResourceDefinition {

    static final String JMX_CAPABILITY = "org.wildfly.management.jmx";

    static final RuntimeCapability<Void> JSR77_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.jsr77")
            .addRequirements(JMX_CAPABILITY)
            .build();

    static final RuntimeCapability<Void> JSR77_APPCLIENT_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.jsr77")
            .build();
    private final boolean appclient;

    JSR77ManagementRootResource(boolean appclient) {
        super(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, JSR77ManagementExtension.SUBSYSTEM_NAME),
                JSR77ManagementExtension.getResourceDescriptionResolver(JSR77ManagementExtension.SUBSYSTEM_NAME),
                new JSR77ManagementSubsystemAdd(appclient), JSR77ManagementSubsystemRemove.INSTANCE);
        this.appclient = appclient;
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptyList();
    }

    @Override
    public void registerCapabilities(ManagementResourceRegistration resourceRegistration) {
        if (appclient) {
            resourceRegistration.registerCapability(JSR77_APPCLIENT_CAPABILITY);
        } else {
            resourceRegistration.registerCapability(JSR77_CAPABILITY);
        }
    }
}
