/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.passivation;

import jakarta.ejb.Local;
import jakarta.ejb.PostActivate;
import jakarta.ejb.PrePassivate;
import jakarta.ejb.Remove;
import jakarta.ejb.Stateful;

import org.jboss.ejb3.annotation.Cache;

/**
 * @author Jaikiran Pai
 */
@Stateful(passivationCapable = false)
@Cache("distributable")
@Local(Bean.class)
public class PassivationDisabledBean implements Bean {

    private boolean prePrePassivateInvoked;
    private boolean postActivateInvoked;

    @PrePassivate
    private void beforePassivate() {
        this.prePrePassivateInvoked = true;
    }

    @PostActivate
    private void afterActivate() {
        this.postActivateInvoked = true;
    }

    @Override
    public boolean wasPassivated() {
        return this.prePrePassivateInvoked;
    }

    @Override
    public boolean wasActivated() {
        return this.postActivateInvoked;
    }

    @Remove
    @Override
    public void close() {
    }
}
