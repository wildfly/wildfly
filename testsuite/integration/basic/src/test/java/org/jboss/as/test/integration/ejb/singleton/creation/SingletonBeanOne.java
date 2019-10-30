/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.singleton.creation;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;

import org.jboss.logging.Logger;

/**
 * The test fixture for {@link SingletonReentrantPostConstructTestCase}.
 *
 * @author steve.coy
 */
@Singleton
public class SingletonBeanOne {

    private static final Logger logger = Logger.getLogger(SingletonBeanOne.class);

    @EJB
    private SingletonBeanTwo beanTwo;

    @Resource
    private SessionContext sessionContext;

    public SingletonBeanOne() {
    }

    /**
     * Grabs a reference to it's own business interface and passes it to {@link #beanTwo}.
     */
    @PostConstruct
    void initialise() {
        logger.trace("Initialising");
        SingletonBeanOne thisBO = sessionContext.getBusinessObject(SingletonBeanOne.class);
        beanTwo.useBeanOne(thisBO);
    }

    /**
     * Stub method to start the bean life-cycle.
     */
    public void start() {
    }

    /**
     * A business method for {@link SingletonBeanTwo} to call.
     */
    public void performSomething() {
        logger.trace("Doing something");
    }

}
