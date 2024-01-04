/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.exception;

import jakarta.ejb.Stateless;

@FooBinding
@Stateless
public class Foo {

    public void doSomething() {
    }
}
