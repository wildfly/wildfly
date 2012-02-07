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

package org.jboss.as.test.integration.ejb.pool.override;

import org.jboss.logging.Logger;

/**
 * @author Jaikiran Pai
 */
public abstract class AbstractSlowBean {

    private static final Logger logger = Logger.getLogger(AbstractSlowBean.class);
    
    public void delay(final long delayInMilliSec) {
        try {
            logger.info("Sleeping for " + delayInMilliSec + " milli seconds");
            Thread.sleep(delayInMilliSec);
            logger.info("Woke up after " + delayInMilliSec + " milli seconds");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
