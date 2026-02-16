/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jpa.hibernate.session;

import java.lang.reflect.Method;

import org.hibernate.StatelessSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Ensures that the Hibernate StatelessSession API conforms to what
 * our proxies using ScopedStatelessSessionInvocationHandler expect.
 */
public class StatelessSessionAPITestCase {

    /**
     * Confirm there is an 'unwrap(Class type)' method and no other 'unwrap' methods.
     */
    @Test
    public void checkUnwrapAPI() {
        boolean foundAnyUnwrap = false;
        boolean foundExpectedUnwrap = false;
        for (Method method : StatelessSession.class.getMethods()) {
            if (method.getName().startsWith("unwrap")) {
                Assertions.assertFalse(foundAnyUnwrap, "More than one unwrap method found");
                foundAnyUnwrap = true;
                foundExpectedUnwrap = method.getParameterCount() == 1 && Class.class.equals(method.getParameterTypes()[0]);
            }
        }

        Assertions.assertTrue(foundExpectedUnwrap, "StatelessSession does not include an unwrap method with a single argument of type Class");
    }
}
