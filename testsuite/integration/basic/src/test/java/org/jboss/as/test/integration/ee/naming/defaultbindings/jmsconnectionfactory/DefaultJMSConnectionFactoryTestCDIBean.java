/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.naming.defaultbindings.jmsconnectionfactory;

import jakarta.annotation.Resource;
import jakarta.jms.ConnectionFactory;

/**
 * @author Eduardo Martins
 */
public class DefaultJMSConnectionFactoryTestCDIBean {

    @Resource
    private ConnectionFactory injectedResource;

    /**
     *
     * @throws Throwable
     */
    public void test() throws Throwable {
        // check injected resource
        if(injectedResource == null) {
            throw new NullPointerException("injected resource");
        }
    }
}
