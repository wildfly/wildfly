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

package org.jboss.as.domain.management.security;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ManagementDescription;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;

/**
 * {@link ResourceDefinition} for a management security realm resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SecurityRealmResourceDefinition extends SimpleResourceDefinition {

    public static final SecurityRealmResourceDefinition INSTANCE = new SecurityRealmResourceDefinition();

    private SecurityRealmResourceDefinition() {
        super(PathElement.pathElement(ModelDescriptionConstants.SECURITY_REALM),
                ManagementDescription.getResourceDescriptionResolver("core.management.security-realm"),
                SecurityRealmAddHandler.INSTANCE, SecurityRealmRemoveHandler.INSTANCE,
                OperationEntry.Flag.RESTART_NONE, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new SecretServerIdentityResourceDefinition());
        resourceRegistration.registerSubModel(new SSLServerIdentityResourceDefinition());
        resourceRegistration.registerSubModel(new TruststoreAuthenticationResourceDefinition());
        resourceRegistration.registerSubModel(new LdapAuthenticationResourceDefinition());
        resourceRegistration.registerSubModel(new PropertiesAuthenticationResourceDefinition());
        resourceRegistration.registerSubModel(new XmlAuthenticationResourceDefinition());
    }
}
