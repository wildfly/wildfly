/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.shared;

public class GlowUtil {

    /**
     * WildFly Glow maven plugin instantiates and scans the deployment prior to the test execution.
     * No server is started at this point and no extra processes should be started at this point.
     * Call this method to ignore part of the deployment initialization.
     */
    public static boolean isGlowScan() {
        return System.getProperties().containsKey("org.wildfly.glow.scan");
    }
}
