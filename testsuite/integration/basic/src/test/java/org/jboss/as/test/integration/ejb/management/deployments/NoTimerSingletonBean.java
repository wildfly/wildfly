/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.management.deployments;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.DependsOn;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * Bean to use in tests of management resources for EJB Singletons.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@Singleton
@Startup
@DependsOn({"ManagedSingletonBean"})
@SecurityDomain("other")
@DeclareRoles(value = {"Role1", "Role2", "Role3"})
@RunAs("Role3")
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@LocalBean
public class NoTimerSingletonBean implements BusinessInterface {

    @Override
    public void doIt() {
        // no-op;
    }

    @Override
    public void remove() {
        // no-op;
    }
}
