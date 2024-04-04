/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.smoke.managedbean;

import jakarta.annotation.ManagedBean;
import jakarta.enterprise.inject.Produces;

/**
 * @author Thomas.Diesler@jboss.com
 */
@ManagedBean("SimpleProducerBean")
public class SimpleProducerBean {

    @Produces
    public int number() {
        return 100;
    }

    @Produces
    public String value() {
        return "value";
    }
}
