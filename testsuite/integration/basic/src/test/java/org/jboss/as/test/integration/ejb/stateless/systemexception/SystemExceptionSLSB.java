/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateless.systemexception;

import jakarta.ejb.Stateless;

/**
 * stateful session bean
 *
 */
@Stateless
public class SystemExceptionSLSB {

    public static final String MESSAGE = "Expected Exception";

    private boolean used = false;

    /**
     * Throw a system exception, but only if an exception has not been thrown before from this bean
     *
     * This should throw an exception every time, as the bean should not be re-used after the system exception
     *
     */
    public void systemException() {
        if(used) {
            return;
        }
        used = true;
        throw new RuntimeException(MESSAGE);
    }
}
