/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.messaging.jms.deployment;

import static org.jboss.as.test.integration.messaging.jms.deployment.BaseConnectionPerSessionMDB.QUEUE_LOOKUP;
import static org.jboss.as.test.integration.messaging.jms.deployment.BaseConnectionPerSessionMDB.SINGLE_CONNECTION_SYSTEM_PROPERTY_NAME;

import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;

/**
 * MDB configured with singleConnection using expression support.
 */
@MessageDriven(
        activationConfig = {
                @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = QUEUE_LOOKUP),
                @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "15"),
                @ActivationConfigProperty(propertyName = "singleConnection", propertyValue = "${" + SINGLE_CONNECTION_SYSTEM_PROPERTY_NAME + ":true}")
        }
)
public class SingleConnectionWithExpressionMDB extends BaseConnectionPerSessionMDB {
}
