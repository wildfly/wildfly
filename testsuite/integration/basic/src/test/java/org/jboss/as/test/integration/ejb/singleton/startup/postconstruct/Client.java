/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.startup.postconstruct;

import org.jboss.logging.Logger;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

/**
 * @author Jan Martiska / jmartisk@redhat.com
 */
@Startup
@Singleton
public class Client {

    private Logger logger = Logger.getLogger(Client.class.getCanonicalName());

    @EJB
    private Controller controller;

    @PostConstruct
    public void postConstruct() {
        logger.trace("Client's PostConstruct called.");
    }

}
