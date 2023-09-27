/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.rar;

import jakarta.resource.cci.Connection;

/**
 * MultipleConnection1
 *
 * @version $Revision: $
 */
public interface MultipleConnection1 extends Connection {
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
