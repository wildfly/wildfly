/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.pool.lifecycle;


import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.jms.JMSException;
import javax.naming.NamingException;

import org.jboss.logging.Logger;


/**
 * @author baranowb
 */
@Stateless
public class PointLessMathBean implements PointlesMathInterface {
    private static final Logger log = Logger.getLogger(PointLessMathBean.class);

    @EJB(lookup = "java:global/pool-ejb-callbacks-singleton/LifecycleTrackerBean!org.jboss.as.test.integration.ejb.pool.lifecycle.LifecycleTracker")
    private LifecycleTracker lifeCycleTracker;

    @Override
    public double pointlesMathOperation(double a, double b, double c) {
        return b * b - 4 * a * c;
    }

    @PostConstruct
    private void postConstruct() throws JMSException, NamingException {
        this.lifeCycleTracker.trackPostConstructOn(this.getClass().getName());
        log.trace("@PostConstruct invoked on " + this);
    }

    @PreDestroy
    private void preDestroy() {
        lifeCycleTracker.trackPreDestroyOn(this.getClass().getName());
        log.trace("@PreDestroy invoked on " + this);
    }
}
