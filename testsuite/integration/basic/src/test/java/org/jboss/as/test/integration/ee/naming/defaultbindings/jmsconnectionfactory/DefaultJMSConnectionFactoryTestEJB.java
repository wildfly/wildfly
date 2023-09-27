/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.naming.defaultbindings.jmsconnectionfactory;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.jms.ConnectionFactory;
import javax.naming.InitialContext;

/**
 * @author Eduardo Martins
 */
@Stateless
public class DefaultJMSConnectionFactoryTestEJB {

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
        // checked jndi lookup
        new InitialContext().lookup("java:comp/DefaultJMSConnectionFactory");
    }

}
