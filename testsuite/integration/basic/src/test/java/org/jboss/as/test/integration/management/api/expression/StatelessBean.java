/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.management.api.expression;

import jakarta.ejb.Stateless;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.logging.Logger;

@Stateless
public class StatelessBean {
    private static final Logger log = Logger.getLogger(StatelessBean.class);

    private ModelControllerClient getClient() {
        return TestSuiteEnvironment.getModelControllerClient();
    }


    public String getJBossProperty(String name) {
        ModelControllerClient client = getClient();
        String result = Utils.getProperty(name, client);
        log.debug("JBoss system property " + name + " was resolved to be " + result);
        return result;
    }

    public void addSystemProperty(String name, String value) {
        System.setProperty(name, value);

    }

    public String getSystemProperty(String name) {
        String result = System.getProperty(name);
        log.debug("System property " + name + " has value " + result);
        return result;
    }

}
