/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.passivation.store;

import org.jboss.as.test.integration.ejb.stateful.passivation.Bean;
import org.jboss.ejb3.annotation.Cache;

import jakarta.ejb.Local;
import jakarta.ejb.PostActivate;
import jakarta.ejb.PrePassivate;
import jakarta.ejb.Remove;
import jakarta.ejb.Stateful;

@Stateful
@Cache("another-passivating-cache")
@Local(Bean.class)
public class DifferentCachePassivationBean implements Bean {

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
