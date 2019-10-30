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
import javax.ejb.Singleton;

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
