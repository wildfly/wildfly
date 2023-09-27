/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
            logger.trace("Sleeping for " + delayInMilliSec + " milliseconds");
            Thread.sleep(delayInMilliSec);
            logger.trace("Woke up after " + delayInMilliSec + " milliseconds");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
