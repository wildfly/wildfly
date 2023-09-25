/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.support.managedbean;

import jakarta.annotation.ManagedBean;
import jakarta.inject.Inject;

import org.jboss.as.test.integration.ee.injection.support.ProducedString;

@ManagedBean("ManagedBeanWithInject")
public class ManagedBeanWithInject {

    private final String name;

    public ManagedBeanWithInject() {
        this.name = null;
    }

    @Inject
    public ManagedBeanWithInject(@ProducedString String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
