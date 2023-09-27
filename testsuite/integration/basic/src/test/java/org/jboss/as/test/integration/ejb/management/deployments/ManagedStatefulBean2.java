/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.management.deployments;

import java.util.concurrent.TimeUnit;
import jakarta.annotation.security.DeclareRoles;
import jakarta.annotation.security.RunAs;
import jakarta.ejb.AfterBegin;
import jakarta.ejb.AfterCompletion;
import jakarta.ejb.BeforeCompletion;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Remove;
import jakarta.ejb.Stateful;
import jakarta.ejb.StatefulTimeout;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;

import org.jboss.ejb3.annotation.Cache;
import org.jboss.ejb3.annotation.SecurityDomain;

@Stateful(passivationCapable = false)
@SecurityDomain("other")
@DeclareRoles(value = {"Role1", "Role2", "Role3"})
@RunAs("Role3")
@Cache("distributable")
@StatefulTimeout(value = 2, unit = TimeUnit.HOURS)
@TransactionManagement(TransactionManagementType.BEAN)
@LocalBean
public class ManagedStatefulBean2 extends AbstractManagedBean implements BusinessInterface {
    @AfterBegin
    private void afterBegin() {
    }

    @BeforeCompletion
    private void beforeCompletion() {
    }

    @AfterCompletion
    private void afterCompletion() {
    }

    @Remove(retainIfException = true)
    public void removeTrue() {
    }

    @Remove(retainIfException = false)
    public void removeFalse() {
    }
}
