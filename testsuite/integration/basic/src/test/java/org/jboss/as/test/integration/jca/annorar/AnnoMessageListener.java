/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.annorar;

/**
 * AnnoMessageListener
 *
 * @version $Revision: $
 */
public interface AnnoMessageListener {
    /**
     * Receive message
     *
     * @param msg String.
     */
    void onMessage(String msg);
}
