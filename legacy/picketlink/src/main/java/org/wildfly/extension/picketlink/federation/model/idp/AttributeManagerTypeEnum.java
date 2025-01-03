/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.picketlink.federation.model.idp;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Enum defining alias for each supported built-in org.picketlink.identity.federation.core.interfaces.AttributeManager provided by
 * PicketLink. The alias is used in the configuration without using the full qualified name of a type.</p>
 *
 * @author Pedro Igor
 */
public enum AttributeManagerTypeEnum {

    UNDERTOW_ATTRIBUTE_MANAGER("UndertowAttributeManager"),
    EMPTY_ATTRIBUTE_MANAGER("EmptyAttributeManager");

    private static final Map<String, AttributeManagerTypeEnum> types = new HashMap<String, AttributeManagerTypeEnum>();

    static {
        for (AttributeManagerTypeEnum element : values()) {
            types.put(element.getAlias(), element);
        }
    }

    private final String alias;

    AttributeManagerTypeEnum(String alias) {
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
