/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.deployment.rar;

/**
 * MultipleConnection2
 *
 * @version $Revision: $
 */
public interface MultipleConnection2 {
    /**
     * test
     *
     * @param s s
     * @return String
     */
    String test(String s);

    /**
     * Close
     */
    void close();
}
