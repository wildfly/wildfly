/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.deployment.dependencies;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;
import jakarta.validation.Validator;

@Singleton
@Startup
public class DependentInjectEjb implements StringView {


    @EJB(lookup = "java:global/dependee/DependeeEjb")
    StringView depdendent;

    @Resource
    Validator validator;
    @Inject
    BeanManager beanManager;

    @PostConstruct
    public void post() throws InterruptedException {
        beanManager.createInstance();
    }

    @Override
    public String getString() {
        return depdendent.getString();
    }
}
