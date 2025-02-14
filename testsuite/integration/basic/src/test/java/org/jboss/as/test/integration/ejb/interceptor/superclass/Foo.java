/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.superclass;

@jakarta.ejb.Local
public interface Foo {

    public void foo(String message);

}
