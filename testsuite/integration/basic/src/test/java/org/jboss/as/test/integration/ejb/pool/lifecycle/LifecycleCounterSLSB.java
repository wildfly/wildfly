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

import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.Stateless;
import javax.jms.JMSException;
import javax.naming.InitialContext;
import javax.naming.NamingException;



/**
 * @author baranowb
 */
@Stateless
public class LifecycleCounterSLSB implements PointlesMathInterface {
    //TODO: If declared as @Singleton, Arquilian throws exception at deployment stating that its already deployed...
    private static final Logger log = Logger.getLogger(LifecycleCounterSLSB.class.getName());

    // TODO: injection here does not work.
    // @EJB
    private LifecycleCounter lifeCycleCounter;

    @Resource
    private SessionContext context;

    @Override
    public double pointlesMathOperation(double a, double b, double c) {
        return b*b - 4*a*c;
    }

    @PreDestroy
    protected void preDestroy() throws JMSException {

        this.log.info("SLSB about to be gone, releasing resources");
        this.lifeCycleCounter.incrementPreDestroyCount();
    }

    @PostConstruct
    protected void postConstruct() throws JMSException, NamingException {
        this.log.info("SLSB created, initializing");

        try {
            this.lifeCycleCounter = (LifecycleCounter) new InitialContext()
                    .lookup("java:global/pool-ejb-callbacks-singleton/LifecycleCounterBean!org.jboss.as.test.integration.ejb.pool.lifecycle.LifecycleCounter");

        } catch (Exception e) {
            e.printStackTrace();
        }
        this.lifeCycleCounter.incrementPostCreateCount();
    }
}
