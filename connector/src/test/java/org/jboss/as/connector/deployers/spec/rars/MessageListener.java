/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.deployers.spec.rars;

/**
 * MessageListener
 *
 * @author <a href="mailto:jeff.zhang@ironjacamar.org">Jeff Zhang</a>
 */
public interface MessageListener {
    /**
     * receive message
     *
     * @param msg String.
     */
    public void onMessage(String msg);
}
