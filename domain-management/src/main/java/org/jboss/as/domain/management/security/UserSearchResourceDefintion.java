/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.domain.management.security.LdapAuthorizationResourceDefinition.LdapAuthorizationChildAddHandler;
import org.jboss.as.domain.management.security.LdapCacheResourceDefinition.CacheFor;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * A {@link org.jboss.as.controller.ResourceDefinition} for standard searches for a users distinguished name.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class UserSearchResourceDefintion extends BaseLdapUserSearchResource {

    public static final SimpleAttributeDefinition ATTRIBUTE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.ATTRIBUTE, ModelType.STRING, false)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, false))
            .setDefaultValue(new ModelNode("uid"))
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    private static final AttributeDefinition[] ATTRIBUTE_DEFINITIONS = {FORCE, BASE_DN, RECURSIVE, USER_DN_ATTRIBUTE, ATTRIBUTE};

    public static final ResourceDefinition INSTANCE = new UserSearchResourceDefintion();

    private UserSearchResourceDefintion() {
        super(UserSearchType.USERNAME_FILTER,
                ControllerResolver.getResolver("core.management.security-realm.authorization.ldap.user-search.username-filter"),
                new LdapAuthorizationChildAddHandler(false, ATTRIBUTE_DEFINITIONS), LdapAuthorizationResourceDefinition.REMOVE_INSTANCE);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerSubModel(LdapCacheResourceDefinition.createByAccessTime(CacheFor.AuthzUser));
        resourceRegistration.registerSubModel(LdapCacheResourceDefinition.createBySearchTime(CacheFor.AuthzUser));
    }

    @Override
    public AttributeDefinition[] getAttributeDefinitions() {
        return ATTRIBUTE_DEFINITIONS;
    }

}
