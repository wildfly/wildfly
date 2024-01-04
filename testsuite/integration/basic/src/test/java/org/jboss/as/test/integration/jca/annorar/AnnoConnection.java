/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.annorar;

/**
 * AnnoConnection
 *
 * @version $Revision: $
 */
public interface AnnoConnection {
    /**
     * Call me
     */
    void callMe();

    /**
     * Close
     */
    void close();
}
