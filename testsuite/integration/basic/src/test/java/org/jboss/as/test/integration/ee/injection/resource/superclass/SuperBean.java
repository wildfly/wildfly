/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.superclass;

import jakarta.annotation.Resource;

/**
 * @author Stuart Douglas
 */
public class SuperBean {

    /**
     * This should create a binding for java:module/env/org.jboss.as.test.integration.injection.resource.superclass.SuperBean/simpleManagedBean
     */
    @Resource(lookup = "java:module/simpleStatelessBean")
    protected SimpleStatelessBean simpleStatelessBean;


    private String simpleString;

    public String getSimpleString() {
        return simpleString;
    }

    @Resource(lookup = "java:module/string1")
    public void setSimpleString(final String simpleString) {
        this.simpleString = simpleString;
    }
}
