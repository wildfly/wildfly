/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.messaging.jms.deployment;

import static org.jboss.as.test.integration.messaging.jms.deployment.BaseConnectionPerSessionMDB.QUEUE_LOOKUP;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;

/**
 * MDB configured with singleConnection=true to share a single connection across all MDB sessions.
 * With maxSession=15, this creates 1 connection with 15 sessions instead of 15 separate connections.
 */
@MessageDriven(
        activationConfig = {
                @ActivationConfigProperty(propertyName = "singleConnection", propertyValue = "true"),
                @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = QUEUE_LOOKUP),
                @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "15")
        }
)
public class SingleConnectionPerSessionMDB extends BaseConnectionPerSessionMDB {
}
