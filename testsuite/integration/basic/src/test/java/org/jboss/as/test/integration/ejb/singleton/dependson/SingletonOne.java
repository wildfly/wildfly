/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.dependson;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;

/**
 * @author Ondrej Chaloupka
 */
@Singleton
public class SingletonOne {
    @EJB(lookup = "java:global/callcounter/CallCounterSingleton")
    CallCounterSingleton callCounter;

    @PostConstruct
    public void postConstruct() {
        callCounter.addCall(SingletonOne.class.getSimpleName());
    }

    @PreDestroy
    public void preDestroy() {
        callCounter.addCall(SingletonOne.class.getSimpleName());
    }
}
