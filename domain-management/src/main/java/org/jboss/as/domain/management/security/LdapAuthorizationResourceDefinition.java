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


import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;

/**
 * {@link ResourceDefinition} for a management security realm's LDAP-based Authorization resource.
 *
 *  @author <a href="mailto:Flemming.Harms@gmail.com">Flemming Harms</a>
 *  @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class LdapAuthorizationResourceDefinition extends LdapResourceDefinition {

    private static final AttributeDefinition[] ATTRIBUTE_DEFINITIONS = { CONNECTION };

    public LdapAuthorizationResourceDefinition() {
        super(PathElement.pathElement(ModelDescriptionConstants.AUTHORIZATION, ModelDescriptionConstants.LDAP),
                ControllerResolver.getResolver("core.management.security-realm.authorization.ldap"),
                new SecurityRealmChildAddHandler(false, true, ATTRIBUTE_DEFINITIONS), new SecurityRealmChildRemoveHandler(true),
                OperationEntry.Flag.RESTART_RESOURCE_SERVICES, OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
    }


    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(UserIsDnResourceDefintion.INSTANCE);
        resourceRegistration.registerSubModel(UserSearchResourceDefintion.INSTANCE);
        resourceRegistration.registerSubModel(AdvancedUserSearchResourceDefintion.INSTANCE);
        resourceRegistration.registerSubModel(GroupToPrincipalResourceDefinition.INSTANCE);
        resourceRegistration.registerSubModel(PrincipalToGroupResourceDefinition.INSTANCE);
    }


    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        SecurityRealmChildWriteAttributeHandler handler = new SecurityRealmChildWriteAttributeHandler(ATTRIBUTE_DEFINITIONS);
        handler.registerAttributes(resourceRegistration);
    }

}
