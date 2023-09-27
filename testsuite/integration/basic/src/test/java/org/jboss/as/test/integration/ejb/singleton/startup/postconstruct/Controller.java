/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.startup.postconstruct;

import org.jboss.logging.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

/**
 * @author Jan Martiska / jmartisk@redhat.com
 */
@Startup
@Singleton
public class Controller {

    private int postConstructInvocationCounter = 0;
    private Logger logger = Logger.getLogger(Controller.class.getCanonicalName());

    @PostConstruct
    public void postConstruct() {
        logger.trace("Controller's PostConstruct called.");
        postConstructInvocationCounter++;
    }

    public int getPostConstructInvocationCounter() {
        return postConstructInvocationCounter;
    }
}
