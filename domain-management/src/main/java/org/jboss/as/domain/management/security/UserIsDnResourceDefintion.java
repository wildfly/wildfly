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
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.domain.management.security.LdapAuthorizationResourceDefinition.LdapAuthorizationChildAddHandler;

/**
 * A {@link org.jboss.as.controller.ResourceDefinition} for user searches where the
 * supplied name is the users DN.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class UserIsDnResourceDefintion extends BaseLdapUserSearchResource {

    private static final AttributeDefinition[] ATTRIBUTE_DEFINITIONS = { FORCE };

    public static final ResourceDefinition INSTANCE = new UserIsDnResourceDefintion();

    private UserIsDnResourceDefintion() {
        super(UserSearchType.USERNAME_IS_DN,
              ControllerResolver.getResolver("core.management.security-realm.authorization.ldap.user-search.username-to-dn"),
              new LdapAuthorizationChildAddHandler(false, ATTRIBUTE_DEFINITIONS), LdapAuthorizationResourceDefinition.REMOVE_INSTANCE);
    }

    @Override
    public AttributeDefinition[] getAttributeDefinitions() {
        return ATTRIBUTE_DEFINITIONS;
    }

}
