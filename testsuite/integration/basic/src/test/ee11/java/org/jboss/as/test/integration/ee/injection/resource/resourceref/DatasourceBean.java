/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.resource.resourceref;

import javax.sql.DataSource;

import jakarta.ejb.Singleton;

/**
 * Used in ResourceRefTestCase to test datasource injection via res-ref.
 * In the ee11 source tree this is an {@code @ManagedBean}, a type that is not available in EE 11.
 *
 * @author Stuart Douglas
 */
@Singleton(name="datasourceManagedBean")
public class DatasourceBean {

    private DataSource ds;

    public DataSource getDataSource() {
        return ds;
    }
}
