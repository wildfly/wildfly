/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.jsp;

/**
 *
 * @author rmartinc
 */
public class DummyBean {

    public static final String DEFAULT_VALUE = "default value";
    private String test = DEFAULT_VALUE;

    public String getTest() {
        return test;
    }

    public void setTest(String test) {
        this.test = test;
    }
}
