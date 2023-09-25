/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.async;

import jakarta.ejb.Asynchronous;

/**
 * @author Ondrej Chaloupka
 */
@Asynchronous
public class AsyncParentClass {
    public static volatile boolean voidMethodCalled = false;
}
