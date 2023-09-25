/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.management.deployments;

import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateful;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;

import org.jboss.ejb3.annotation.Cache;
import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * Bean to use in tests of management resources for SFSBs.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
@Stateful
@SecurityDomain("other")
@DeclareRoles(value = {"Role1", "Role2", "Role3"})
@RunAs("Role3")
@Cache("distributable")
@TransactionManagement(TransactionManagementType.BEAN)
@LocalBean
public class ManagedStatefulBean extends AbstractManagedBean implements BusinessInterface {
}
