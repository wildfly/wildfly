/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.superclass;

import jakarta.ejb.Local;
import jakarta.ejb.Stateless;

@Stateless(name = "FooBean")
@Local(Foo.class)
public class FooBean extends AbstractFooBean implements Foo {

}
