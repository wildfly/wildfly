/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.single.ejb.stateful.passivation.bean;

import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;

import org.jboss.as.test.clustering.single.ejb.stateful.bean.IncrementorBean;

import jakarta.ejb.PostActivate;
import jakarta.ejb.PrePassivate;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateful;

/**
 * A stateful session bean that records passivation and activation events. Used for testing idle time-based passivation with
 * idle-threshold configuration.
 *
 * @author Radoslav Husar
 */
@Stateful
@Remote(PassivatingIncrementor.class)
public class PassivatingIncrementorBean extends IncrementorBean implements PassivatingIncrementor {

    private final Deque<Instant> activations = new LinkedBlockingDeque<>();
    private final Deque<Instant> passivations = new LinkedBlockingDeque<>();

    @Override
    public Deque<Instant> getActivations() {
        return this.activations;
    }

    @Override
    public Deque<Instant> getPassivations() {
        return this.passivations;
    }

    @PrePassivate
    public void prePassivate() {
        System.out.println("Received @PrePassivate event for " + PassivatingIncrementorBean.class.getName());
        this.passivations.add(Instant.now());
    }

    @PostActivate
    public void postActivate() {
        System.out.println("Received @PostActivate event for " + PassivatingIncrementorBean.class.getName());
        this.activations.add(Instant.now());
    }
}
