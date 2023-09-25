/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.startup;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import org.jboss.logging.Logger;

/**
 * User: jpai
 */
@Startup
@Singleton
public class StartupSingletonA {

    private static Logger logger = Logger.getLogger(StartupSingletonA.class);

    @EJB(beanName = "SLSBOne")
    private DoSomethingView slsbOne;

    @PostConstruct
    public void onConstruct() throws Exception {
        this.slsbOne.doSomething();
    }

    public void doSomething() {
        logger.trace(this.getClass().getName() + "#doSomething()");
    }

    public String echo(String msg) {
        logger.trace("Echo " + msg);
        return msg;
    }

}
