/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.federation.model.idp;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Enum defining alias for each supported built-in org.picketlink.identity.federation.core.interfaces.RoleGenerator provided by
 * PicketLink. The alias is used in the configuration without using the full qualified name of a type.</p>
 *
 * @author Pedro Igor
 */
public enum RoleGeneratorTypeEnum {

    UNDERTOW_ROLE_GENERATOR("UndertowRoleGenerator"),
    EMPTY_ROLE_GENERATOR("EmptyRoleGenerator");

    private static final Map<String, RoleGeneratorTypeEnum> types = new HashMap<String, RoleGeneratorTypeEnum>();

    static {
        for (RoleGeneratorTypeEnum element : values()) {
            types.put(element.getAlias(), element);
        }
    }

    private final String alias;

    RoleGeneratorTypeEnum(String alias) {
        this.alias = alias;
    }

    @Override
    public String toString() {
        return this.alias;
    }

    String getAlias() {
        return this.alias;
    }
}
