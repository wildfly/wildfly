/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.weld.jndi;

import jakarta.annotation.Resource;
import jakarta.enterprise.inject.Produces;

/**
 * @author Stuart Douglas
 */
public class AppNameProducer {

    @Produces
    @Resource(mappedName="java:app/AppName")
    public String name;
}
