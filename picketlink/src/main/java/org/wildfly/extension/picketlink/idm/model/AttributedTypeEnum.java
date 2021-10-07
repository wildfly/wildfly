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

package org.wildfly.extension.picketlink.idm.model;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Enum defining alias for each supported built-in {@code org.picketlink.idm.model.AttributedType} provided by PicketLink. The
 * alias is used in the configuration without using the full qualified name of a type.</p>
 *
 * @author Pedro Igor
 */
public enum AttributedTypeEnum {

    // base types
    PARTITION("Partition", "org.picketlink.idm.model.Partition"),
    IDENTITY_TYPE("IdentityType", "org.picketlink.idm.model.IdentityType"),
    ACCOUNT("Account", "org.picketlink.idm.model.Account"),
    RELATIONSHIP("Relationship", "org.picketlink.idm.model.Relationship"),
    PERMISSION("Permission", "org.picketlink.idm.permission.Permission"),

    // basic model types
    REALM("Realm", "org.picketlink.idm.model.basic.Realm"),
    TIER("Tier", "org.picketlink.idm.model.basic.Tier"),
    AGENT("Agent", "org.picketlink.idm.model.basic.Agent"),
    USER("User", "org.picketlink.idm.model.basic.User"),
    ROLE("Role", "org.picketlink.idm.model.basic.Role"),
    GROUP("Group", "org.picketlink.idm.model.basic.Group"),
    GRANT("Grant", "org.picketlink.idm.model.basic.Grant"),
    GROUP_ROLE("GroupRole", "org.picketlink.idm.model.basic.GroupRole"),
    GROUP_MEMBERSHIP("GroupMembership", "org.picketlink.idm.model.basic.GroupMembership");

    private static final Map<String, AttributedTypeEnum> types = new HashMap<String, AttributedTypeEnum>();

    static {
        for (AttributedTypeEnum element : values()) {
            types.put(element.getAlias(), element);
        }
    }

    private final String alias;
    private final String type;

    AttributedTypeEnum(String alias, String type) {
        this.alias = alias;
        this.type = type;
    }

    public static String forType(String alias) {
        AttributedTypeEnum resolvedType = types.get(alias);

        if (resolvedType != null) {
            return resolvedType.getType();
        }

        return null;
    }

    @Override
    public String toString() {
        return this.alias;
    }

    public String getAlias() {
        return this.alias;
    }

    String getType() {
        return this.type;
    }
}
