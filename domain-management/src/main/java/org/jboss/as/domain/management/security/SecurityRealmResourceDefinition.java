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

import java.util.List;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link ResourceDefinition} for a management security realm resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SecurityRealmResourceDefinition extends SimpleResourceDefinition {

    public static final SecurityRealmResourceDefinition INSTANCE = new SecurityRealmResourceDefinition();

    public static final SimpleAttributeDefinition MAP_GROUPS_TO_ROLES = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.MAP_GROUPS_TO_ROLES, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(true))
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    private final List<AccessConstraintDefinition> sensitivity;

    private SecurityRealmResourceDefinition() {
        super(PathElement.pathElement(ModelDescriptionConstants.SECURITY_REALM),
                ControllerResolver.getResolver("core.management.security-realm"),
                SecurityRealmAddHandler.INSTANCE,
                SecurityRealmRemoveHandler.INSTANCE,
                OperationEntry.Flag.RESTART_NONE,
                OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
        sensitivity = SensitiveTargetAccessConstraintDefinition.SECURITY_REALM.wrapAsList();
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(MAP_GROUPS_TO_ROLES, null, new ReloadRequiredWriteAttributeHandler(MAP_GROUPS_TO_ROLES));
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(new PlugInResourceDefinition());
        resourceRegistration.registerSubModel(new SecretServerIdentityResourceDefinition());
        resourceRegistration.registerSubModel(new SSLServerIdentityResourceDefinition());
        resourceRegistration.registerSubModel(new TruststoreAuthenticationResourceDefinition());
        resourceRegistration.registerSubModel(new LocalAuthenticationResourceDefinition());
        resourceRegistration.registerSubModel(new JaasAuthenticationResourceDefinition());
        resourceRegistration.registerSubModel(new LdapAuthenticationResourceDefinition());
        resourceRegistration.registerSubModel(new PropertiesAuthenticationResourceDefinition());
        resourceRegistration.registerSubModel(new XmlAuthenticationResourceDefinition());
        resourceRegistration.registerSubModel(new PlugInAuthenticationResourceDefinition());
        resourceRegistration.registerSubModel(new PropertiesAuthorizationResourceDefinition());
        resourceRegistration.registerSubModel(new PlugInAuthorizationResourceDefinition());
        resourceRegistration.registerSubModel(new LdapAuthorizationResourceDefinition());
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return sensitivity;
    }
}
