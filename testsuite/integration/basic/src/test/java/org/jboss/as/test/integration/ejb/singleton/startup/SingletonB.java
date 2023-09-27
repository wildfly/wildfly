/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.startup;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Remote;
import jakarta.ejb.Singleton;

import org.jboss.logging.Logger;

/**
 * User: jpai
 */
@Singleton
@Remote(SingletonBeanRemoteView.class)
public class SingletonB implements SingletonBeanRemoteView {

    private static Logger logger = Logger.getLogger(SingletonB.class);

    @EJB(beanName = "SLSBTwo")
    private DoSomethingView slsbTwo;

    @PostConstruct
    public void onConstruct() throws Exception {
        slsbTwo.doSomething();
    }

    public void doSomething() {
        logger.trace(this.getClass().getName() + "#doSomething()");
    }

    @Override
    public String echo(String msg) {
        logger.trace("Echo " + msg);
        return msg;
    }
}
