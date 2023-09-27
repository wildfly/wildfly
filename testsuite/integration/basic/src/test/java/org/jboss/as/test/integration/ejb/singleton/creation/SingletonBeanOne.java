/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.creation;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Singleton;

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
