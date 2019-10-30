/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.ejb.pool.lifecycle;


import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.jms.JMSException;
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
