/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.regex;

import jakarta.interceptor.ExcludeClassInterceptors;
import jakarta.interceptor.ExcludeDefaultInterceptors;

/**
 * @author Stuart Douglas
 */
public class TestEjb {
    public static final String MESSAGE = "test";

    public String test() {
        return MESSAGE;
    }

    @ExcludeDefaultInterceptors
    public String testIgnoreDefault() {
        return MESSAGE;
    }

    @ExcludeClassInterceptors
    public String testIgnoreClass() {
        return MESSAGE;
    }
}
