/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.shared;

import org.junit.Assume;

/**
 * Utility class to disable tests failing when Security Manager is enabled.
 *
 * Important note: this should be used only in cases when tests are failing due to a thirdparty issues which are
 * unlikely to get fixed, e.g. WFLY-6192.
 *
 * @author Ivo Studensky
 */
public final class SecurityManagerFailure {

    /**
     * This method can be used to disable tests that are failing when being run under Security Manager.
     *
     * The purpose to calling this method (as opposed to providing additional permissions which might hide the
     * actual problem permanently) is to be able to enable or disable the test based on the presence
     * of the {@code jboss.test.enableTestsFailingUnderSM} system property.
     *
     * To run tests disabled by this method, you must add {@code -Djboss.test.enableTestsFailingUnderSM} argument
     * to the JVM running the tests.
     *
     * @param message an optional description about disabling this test (e.g. it can contain a WFLY issue).
     */
    public static void thisTestIsFailingUnderSM(String message) {
        final SecurityManager sm = System.getSecurityManager();
        final boolean securityManagerEnabled = System.getProperty("security.manager") != null;
        // either System.getSecurityManager is not null or system property 'security.manager' is set (in cases of RunAsClient)
        if (sm != null || securityManagerEnabled) {
            final boolean enableTest = System.getProperty("jboss.test.enableTestsFailingUnderSM") != null;
            Assume.assumeTrue(message, enableTest);
        }
    }

    /**
     * @see #thisTestIsFailingUnderSM(String)
     */
    public static void thisTestIsFailingUnderSM() {
        thisTestIsFailingUnderSM("");
    }

}
