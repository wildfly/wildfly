/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.management.deployments;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.ActivationConfigProperty;
import jakarta.ejb.MessageDriven;
import jakarta.ejb.Schedule;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * Bean to use in tests of management resources for MDBs.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@JMSDestinationDefinition(
        name="java:/queue/ManagedMDB-queue",
        interfaceName = "jakarta.jms.Queue"
)
@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "java:/queue/ManagedMDB-queue")
})
@SecurityDomain("other")
@DeclareRoles(value = {"Role1", "Role2", "Role3"})
@RunAs("Role3")
public class ManagedMDB implements MessageListener {

    @Override
    public void onMessage(Message message) {
        // no-op
    }

    @Timeout
    @Schedule(second="15", persistent = false, info = "timer1")
    public void timeout(final Timer timer) {
        // no-op
    }
}

