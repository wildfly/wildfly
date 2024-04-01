/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.support.managedbean;

import jakarta.annotation.ManagedBean;
import jakarta.inject.Inject;

import org.jboss.as.test.integration.ee.injection.support.AroundConstructBinding;
import org.jboss.as.test.integration.ee.injection.support.ProducedString;

@AroundConstructBinding
@ManagedBean("ComplicatedManagedBean")
public class ComplicatedManagedBean {

    private final String name;

    public ComplicatedManagedBean() {
        this.name = null;
    }

    @Inject
    public ComplicatedManagedBean(@ProducedString String name) {
        this.name = name + "#ComplicatedManagedBean";
    }

    public String getName() {
        return name;
    }
}
