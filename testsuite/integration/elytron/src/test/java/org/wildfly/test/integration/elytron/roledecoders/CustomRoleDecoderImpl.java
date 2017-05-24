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

package org.wildfly.test.integration.elytron.roledecoders;

import java.util.HashSet;
import java.util.Map;

import org.wildfly.extension.elytron.Configurable;
import org.wildfly.security.authz.Attributes;
import org.wildfly.security.authz.AuthorizationIdentity;
import org.wildfly.security.authz.RoleDecoder;
import org.wildfly.security.authz.Roles;

public class CustomRoleDecoderImpl implements RoleDecoder, Configurable {

    private String roleAttr;

    @Override
    public Roles decodeRoles(AuthorizationIdentity authorizationIdentity) {
        final Attributes.Entry entry = authorizationIdentity.getAttributes().get(roleAttr);
            return entry.isEmpty() ? Roles.NONE
                : entry instanceof Attributes.SetEntry ? Roles.fromSet((Attributes.SetEntry) entry)
                    : Roles.fromSet(new HashSet<>(entry));
    }

    @Override
    public void initialize(Map<String, String> configuration) {
        if (configuration.containsKey("throwException")) {
            throw new IllegalStateException("Only test purpose. This exception was thrown on demand.");
        }
        roleAttr = configuration.getOrDefault("roleAttr", null);
    }
}
