/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.producer;

import jakarta.annotation.ManagedBean;
import jakarta.inject.Inject;

/**
 * @author Thomas.Diesler@jboss.com
 * @since 09-Jul-2012
 */
@ManagedBean
public class SimpleManagedBean {

    @Inject String driverName;

    public String getDriverName() {
        return driverName;
    }
}
