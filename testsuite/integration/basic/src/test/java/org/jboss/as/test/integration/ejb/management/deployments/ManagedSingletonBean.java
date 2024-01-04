/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.management.deployments;

import static jakarta.ejb.LockType.WRITE;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Lock;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * TBean to use in tests of management resources for EJB Singletons.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@Singleton
@SecurityDomain("other")
@DeclareRoles(value = {"Role1", "Role2", "Role3"})
@RunAs("Role3")
@Lock(WRITE)
@LocalBean
public class ManagedSingletonBean extends AbstractManagedBean implements BusinessInterface {
    @Timeout
    @Schedule(second="15", persistent = false, info = "timer1")
    public void timeout(final Timer timer) {
        // no-op
    }
}
