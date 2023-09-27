/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.naming.defaultbindings.datasource;

import jakarta.annotation.Resource;
import javax.sql.DataSource;

/**
 * @author Eduardo Martins
 */
public class DefaultDataSourceTestCDIBean {

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
    }
}
