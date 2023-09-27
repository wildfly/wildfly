/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.jndi;

import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

/**
 * @author Stuart Douglas
 */
@Singleton
@Startup
public class StartupSingletonEjb {

    @Inject
    private String appName;

    @Resource(lookup="java:comp/BeanManager")
    private BeanManager beanManager;



    public String getName() {
        return appName;
    }

    public BeanManager getBeanManager() {
        return beanManager;

    }
}
