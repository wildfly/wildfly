/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.single.ejb.stateful.passivation.bean;

import java.util.UUID;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.PostActivate;
import jakarta.ejb.PrePassivate;
import jakarta.ejb.Remote;
import jakarta.ejb.Stateful;

import org.jboss.as.test.clustering.PassivationEventTrackerUtil;
import org.jboss.as.test.clustering.single.ejb.stateful.bean.IncrementorBean;

/**
 * A stateful session bean that records passivation and activation events.
 * Used for testing idle time-based passivation with idle-threshold configuration.
 *
 * @author Radoslav Husar
 */
@Stateful
@Remote(PassivatingIncrementor.class)
public class PassivatingIncrementorBean extends IncrementorBean implements PassivatingIncrementor {

    private String identifier;

    @PostConstruct
    public void postConstruct() {
        this.identifier = UUID.randomUUID().toString();
        System.out.println("Called postConstruct() - identifier: " + this.identifier);
    }

    @Override
    public String getIdentifier() {
        return this.identifier;
    }

    @PrePassivate
    public void prePassivate() {
        System.out.println("Called @PrePassivate - identifier: " + this.identifier);
        PassivationEventTrackerUtil.recordPassivation(this.identifier);
    }

    @PostActivate
    public void postActivate() {
        System.out.println("Called @PostActivate - identifier: " + this.identifier);
        PassivationEventTrackerUtil.recordActivation(this.identifier);
    }
}
