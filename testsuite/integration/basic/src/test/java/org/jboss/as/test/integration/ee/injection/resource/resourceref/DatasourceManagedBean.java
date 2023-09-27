/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.resourceref;

import jakarta.annotation.ManagedBean;
import javax.sql.DataSource;

/**
 * @author Stuart Douglas
 */
@ManagedBean("datasourceManagedBean")
public class DatasourceManagedBean {

    private DataSource ds;

    public DataSource getDataSource() {
        return ds;
    }
}
