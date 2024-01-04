/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.interceptor.exception;

import jakarta.ejb.Stateful;

@BarBinding
@Stateful
public class Bar {

    public void doSomething() {
    }
}
