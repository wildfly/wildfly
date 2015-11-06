/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
 * Utility class to disable intermittently failing tests.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2015 Red Hat inc.
 */
public final class IntermittentFailure {

    /**
     * This method can be used to disable tests that are failing intermittently.
     *
     * The purpose to calling this method (as opposed to annotating tests with {@code @Ignore} is to
     * be able to enable or disable the test based on the presence of the {@code jboss.test.enableIntermittentFailingTests}
     * System property.
     *
     * To run tests disabled by this method, you must add {@code -Djboss.test.enableIntermittentFailingTests} argument
     * to the JVM running the tests.
     *
     * @param message a optional description about disabling this test (e.g. it can contain a WFLY issue).
     */
    public static void thisTestIsFailingIntermittently(String message) {
        boolean enableTest = System.getProperty("jboss.test.enableIntermittentFailingTests") != null;
        Assume.assumeTrue(message, enableTest);
    }

    /**
     * @see #thisTestIsFailingIntermittently(String)
     */
    public static void thisTestIsFailingIntermittently() {
        thisTestIsFailingIntermittently("");
    }
}
