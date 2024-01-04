/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.naming.defaultbindings.datasource;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import javax.naming.InitialContext;
import javax.sql.DataSource;

/**
 * @author Eduardo Martins
 */
@Stateless
public class DefaultDataSourceTestEJB {

    @Resource
    private DataSource injectedResource;

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
        new InitialContext().lookup("java:comp/DefaultDataSource");
    }

}
