/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.superclass;

import jakarta.annotation.ManagedBean;

/**
 * @author Stuart Douglas
 */
@ManagedBean("bean1")
public class Bean1 extends SuperBean {

    public SimpleManagedBean getBean() {
        return simpleManagedBean;
    }

    /**
     * We override the superclass method. These should be no injection done
     * @param simpleString
     */
    public void setSimpleString(final String simpleString) {
        super.setSimpleString(simpleString);
    }
}
