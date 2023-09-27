/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.shared;

import org.junit.Assume;

/**
 * Utility class to disable (effectively @Ignore) intermittently failing tests unless explicitly enabled via a System property.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
public final class IntermittentFailure {

    /**
     * This method can be used to disable tests that are failing intermittently.
     *
     * The purpose to calling this method (as opposed to annotating tests with {@code @Ignore}) is to
     * be able to enable or disable the test based on the presence of the {@code jboss.test.enableIntermittentFailingTests}
     * System property.
     *
     * To run tests disabled by this method, you must add {@code -Djboss.test.enableIntermittentFailingTests} argument
     * to the JVM running the tests.
     *
     * @param message reason for disabling this test, typically specifying a tracking WFLY issue
     */
    public static void thisTestIsFailingIntermittently(String message) {
        boolean enableTest = System.getProperty("jboss.test.enableIntermittentFailingTests") != null;
        Assume.assumeTrue(message, enableTest);
    }

}
