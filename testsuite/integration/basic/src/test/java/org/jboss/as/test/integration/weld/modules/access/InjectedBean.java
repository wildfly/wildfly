/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.modules.access;

import jakarta.inject.Inject;

public class InjectedBean {

    @Inject
    private BuiltInBeanWithPackagePrivateConstructor bean;

    public BuiltInBeanWithPackagePrivateConstructor getBean() {
        return bean;
    }
}
