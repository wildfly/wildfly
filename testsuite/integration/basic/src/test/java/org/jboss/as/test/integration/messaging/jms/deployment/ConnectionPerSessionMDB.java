/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.messaging.jms.deployment;

import static org.jboss.as.test.integration.messaging.jms.deployment.BaseConnectionPerSessionMDB.QUEUE_LOOKUP;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;

/**
 * MDB configured with singleConnection=false to create multiple connections (one per MDB session).
 */
@MessageDriven(
        activationConfig = {
                @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = QUEUE_LOOKUP),
                @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "15"),
                @ActivationConfigProperty(propertyName = "singleConnection", propertyValue = "false")
        }
)
public class ConnectionPerSessionMDB extends BaseConnectionPerSessionMDB {
}
