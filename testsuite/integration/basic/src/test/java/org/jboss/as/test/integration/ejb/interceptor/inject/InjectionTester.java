/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.interceptor.inject;

/**
 * InjectionTester
 *
 * @author Jaikiran Pai
 */
public interface InjectionTester {
    /**
     * Checks that all the expected fields/methods have been injected
     *
     * @throws IllegalStateException If any of the expected field/method was not injected
     */
    void assertAllInjectionsDone() throws IllegalStateException;
}
