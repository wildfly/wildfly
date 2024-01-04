/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.creation;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;

import org.jboss.logging.Logger;

/**
 * Part of the test fixture for {@link SingletonReentrantPostConstructTestCase}.
 *
 * @author steve.coy
 */
@Singleton
public class SingletonBeanTwo {

    private static final Logger logger = Logger.getLogger(SingletonBeanTwo.class);

    @PostConstruct
    void initialise() {
        logger.trace("initialised");
    }

    /**
     * Performs a callback on beanOne. This method is called from {@link SingletonBeanOne#initialise()}, so the SingletonBeanOne
     * instance is not yet in the "method-ready" state, making the callback illegal. <p>
     * This resulted in a stack overflow prior to the AS7-5184 fix.
     *
     * @param beanOne
     */
    public void useBeanOne(SingletonBeanOne beanOne) {
        beanOne.performSomething();
    }

}
