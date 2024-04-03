/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.superclass;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@Stateless(name="bean2")
public class Bean2 extends SuperBean {

    public SimpleStatelessBean getBean() {
        return simpleStatelessBean;
    }

    int setCount = 0;

    @Resource(lookup = "java:module/string2")
    public void setSimpleString(final String simpleString) {
        super.setSimpleString(simpleString);
        //keep a count to make sure this is not injected twice
        ++setCount;
    }

    public int getSetCount() {
        return setCount;
    }
}
