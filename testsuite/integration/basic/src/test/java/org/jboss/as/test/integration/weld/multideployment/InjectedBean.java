/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.multideployment;

import jakarta.inject.Inject;

public class InjectedBean {

    @Inject
    private SimpleBean bean;

    public SimpleBean getBean() {
        return bean;
    }
}
