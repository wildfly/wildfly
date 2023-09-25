/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.messaging.mgmt;

import jakarta.jms.JMSException;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2016 Red Hat inc.
 */

public interface RemoteConnectionHolding {
    void createConnection() throws JMSException;

    void closeConnection() throws JMSException;
}
