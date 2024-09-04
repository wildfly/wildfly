/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.cleanup;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class WiremockCleanup {

    public static void main(String[] args) {
        for (String arg : args) {
            if (arg.isBlank()) {
                continue;
            }
            System.out.printf("Cleaning up system property: %s=%s%n", arg, System.getProperty(arg));
            System.clearProperty(arg);
        }
    }
}
