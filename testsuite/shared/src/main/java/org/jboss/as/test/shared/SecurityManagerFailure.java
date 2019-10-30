/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
