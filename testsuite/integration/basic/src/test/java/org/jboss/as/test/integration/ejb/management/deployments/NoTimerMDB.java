/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.management.deployments;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RunAs;
import jakarta.jms.JMSDestinationDefinition;
import jakarta.jms.Message;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * MDB to use in tests of management resources for MDBs. Other bean metadata is declared in ejb-jar.xml
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@JMSDestinationDefinition(
        name="java:/queue/NoTimerMDB-queue",
        interfaceName = "jakarta.jms.Queue"
)
@SecurityDomain("other")
@DeclareRoles(value = {"Role1", "Role2", "Role3"})
@RunAs("Role3")
public class NoTimerMDB {

    public void onMessage(Message message) {
        // no-op
    }
}
