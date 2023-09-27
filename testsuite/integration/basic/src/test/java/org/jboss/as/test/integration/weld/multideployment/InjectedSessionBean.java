/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.multideployment;

import jakarta.ejb.Stateful;
import jakarta.inject.Inject;

@Stateful
public class InjectedSessionBean {

    @Inject
    private SimpleBean bean;

    public SimpleBean getBean() {
        return bean;
    }
}
