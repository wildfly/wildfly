/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.method;

public class AbstractClassifiedBean {
    public String overridedMethod(String a, String b) {
        return "StrStr " + a + " " + b;
    }
}
