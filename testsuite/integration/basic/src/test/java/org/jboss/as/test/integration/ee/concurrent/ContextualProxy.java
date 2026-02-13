/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.concurrent;

/**
 * The interface for the test's contextual proxy.
 * @author emartins
 */
public interface ContextualProxy {

    class DeclaredException extends Exception {
    }

    void test() throws DeclaredException;
}
