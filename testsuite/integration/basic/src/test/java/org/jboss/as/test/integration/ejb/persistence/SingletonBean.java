/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.persistence;

import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceContext;

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
