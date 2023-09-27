/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.exception;

import jakarta.ejb.Stateful;

@BafBinding
@Stateful
public class Baf {

    public void doSomething() {
    }
}
