/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.security.common.ejb3;

/**
 * An interface for basic Jakarta Enterprise Beans tests.
 *
 * @author Josef Cacek
 */
public interface Hello {

    /**
     * Returns a greeting.
     *
     * @return
     */
    String sayHelloWorld();

    /**
     * Returns returns greeting with caller principal name.
     *
     * @return
     */
    String sayHello();

}
