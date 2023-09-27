/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.management.deployments;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Schedule;
import jakarta.ejb.Stateless;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;

import org.jboss.ejb3.annotation.Pool;
import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * Bean to use in tests of management resources for SLSBs.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@Stateless
@SecurityDomain("other")
@DeclareRoles(value = {"Role1", "Role2", "Role3"})
@RunAs("Role3")
@Pool("slsb-strict-max-pool")
@LocalBean
public class ManagedStatelessBean extends AbstractManagedBean implements BusinessInterface {
    @Timeout
    @Schedule(second="15", persistent = false, info = "timer1")
    public void timeout(final Timer timer) {
        // no-op
    }
}
