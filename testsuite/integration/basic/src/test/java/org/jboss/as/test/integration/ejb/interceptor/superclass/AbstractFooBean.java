/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.superclass;

public abstract class AbstractFooBean implements Foo {

    @Override
    public void foo(String message) {
    }

}
