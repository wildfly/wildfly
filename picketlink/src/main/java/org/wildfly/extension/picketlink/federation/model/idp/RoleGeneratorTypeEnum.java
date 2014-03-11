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

package org.wildfly.extension.picketlink.federation.model.idp;

import org.picketlink.identity.federation.bindings.wildfly.idp.UndertowRoleGenerator;
import org.picketlink.identity.federation.core.impl.EmptyRoleGenerator;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Enum defining alias for each supported built-in {@link org.wildfly.extension.picketlink.federation.model.idp.RoleGeneratorTypeEnum} provided by
 * PicketLink. The alias is used in the configuration without using the full qualified name of a type.</p>
 *
 * @author Pedro Igor
 */
public enum RoleGeneratorTypeEnum {

    UNDERTOW_ROLE_GENERATOR("UndertowRoleGenerator", UndertowRoleGenerator.class.getName()),
    EMPTY_ROLE_GENERATOR("EmptyRoleGenerator", EmptyRoleGenerator.class.getName());

    private static final Map<String, RoleGeneratorTypeEnum> types = new HashMap<String, RoleGeneratorTypeEnum>();

    static {
        for (RoleGeneratorTypeEnum element : values()) {
            types.put(element.getAlias(), element);
        }
    }

    private final String alias;
    private final String type;

    private RoleGeneratorTypeEnum(String alias, String type) {
        this.alias = alias;
        this.type = type;
    }

    static String forType(String alias) {
        RoleGeneratorTypeEnum resolvedType = types.get(alias);

        if (resolvedType != null) {
            return resolvedType.getType();
        }

        return null;
    }

    @Override
    public String toString() {
        return this.alias;
    }

    String getAlias() {
        return this.alias;
    }

    String getType() {
        return this.type;
    }
}
