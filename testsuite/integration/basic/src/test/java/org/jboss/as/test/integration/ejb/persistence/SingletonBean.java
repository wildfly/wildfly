/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.persistence;

import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;

import org.jboss.logging.Logger;

/**
 * User: jpai
 */
@Singleton
public class SingletonBean {

    private static final Logger logger = Logger.getLogger(SingletonBean.class);

    @PersistenceContext(unitName = "ejb3-persistence-test-pu")
    private EntityManager entityManager;

    public void doNothing() {
    }

    @PreDestroy
    private void sleepAndDestroy() throws Exception {
        logger.trace("Sleeping for 3 seconds while destroying singleton bean " + this);
        // sleep for a while just to reproduce a race condition with EntityManagerFactory being closed before
        // the singleton bean can finish its pre-destroy
        Thread.sleep(3000);
        logger.trace("Woke up after 3 seconds while destroying singleton bean " + this);
        final EntityManagerFactory entityManagerFactory = this.entityManager.getEntityManagerFactory();
        boolean emFactoryOpen = entityManagerFactory.isOpen();
        if (!emFactoryOpen) {
            throw new RuntimeException("Entitymanager factory: " + entityManagerFactory + " has been closed " +
                    "even before singleton bean " + this + " could complete its @PreDestroy");
        }

    }
}
