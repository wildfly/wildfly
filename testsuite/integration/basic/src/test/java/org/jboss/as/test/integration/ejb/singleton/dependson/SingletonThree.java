/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton.dependson;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.DependsOn;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

/**
 * @author Ondrej Chaloupka
 */
@DependsOn({"SingletonTwo"})
@Singleton
@Startup
public class SingletonThree {
    @EJB(lookup = "java:global/callcounter/CallCounterSingleton")
    CallCounterSingleton callCounter;

    @PostConstruct
    public void postConstruct() {
        callCounter.addCall(SingletonThree.class.getSimpleName());
    }

    @PreDestroy
    public void preDestroy() {
        callCounter.addCall(SingletonThree.class.getSimpleName());
    }
}
