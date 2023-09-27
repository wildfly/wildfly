/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.concurrency;

/**
 * Business interface of an EJB capable of greeting a user
 *
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 */
public interface GreeterCommonBusiness {

    /**
     * Prefix that will be used in {@link GreeterCommonBusiness#greet(String)}
     */
    String PREFIX = "Hello, ";

    /**
     * Greets the user by prepending the specified name with the {@link GreeterCommonBusiness#PREFIX}
     *
     * @param name
     * @return
     */
    String greet(String name);

}
