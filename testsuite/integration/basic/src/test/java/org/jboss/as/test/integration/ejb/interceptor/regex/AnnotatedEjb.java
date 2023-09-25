/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.regex;

import jakarta.ejb.Stateless;

/**
 * @author Ondra Chaloupka <ochaloup@redhat.com>
 */
@Stateless(name = "org.jboss.as.test.integration.ejb.interceptor.regex.AnnotatedEjb")
public class AnnotatedEjb {
    public String test() {
        return TestEjb.MESSAGE;
    }
}
