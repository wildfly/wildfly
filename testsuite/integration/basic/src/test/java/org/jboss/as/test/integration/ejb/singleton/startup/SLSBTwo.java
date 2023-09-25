/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.startup;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Local;
import jakarta.ejb.Stateless;

import org.jboss.logging.Logger;

/**
 * User: jpai
 */
@Stateless
@Local(DoSomethingView.class)
public class SLSBTwo implements DoSomethingView {

    private static Logger logger = Logger.getLogger(SLSBTwo.class);

    @PostConstruct
    public void onConstruct() throws Exception {
        this.doSomething();
    }

    @Override
    public void doSomething() {
        logger.trace(this.getClass().getName() + "#doSomething()");
    }

}
