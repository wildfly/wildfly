/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.superclass;

import jakarta.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@Stateless(name="bean1")
public class Bean1 extends SuperBean {

    public SimpleStatelessBean getBean() {
        return simpleStatelessBean;
    }

    /**
     * We override the superclass method. These should be no injection done
     * @param simpleString the string
     */
    public void setSimpleString(final String simpleString) {
        super.setSimpleString(simpleString);
    }
}
