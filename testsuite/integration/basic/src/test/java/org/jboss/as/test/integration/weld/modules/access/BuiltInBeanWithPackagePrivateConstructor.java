/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.modules.access;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class BuiltInBeanWithPackagePrivateConstructor {

    private final AtomicReference<String> value;

    BuiltInBeanWithPackagePrivateConstructor() {
        this.value = new AtomicReference<>();
    }

    public String getValue() {
        return value.get();
    }

    public void setValue(String value) {
        this.value.set(value);
    }
}
