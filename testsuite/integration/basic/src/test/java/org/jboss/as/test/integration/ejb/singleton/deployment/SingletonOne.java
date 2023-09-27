/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.singleton.deployment;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

/**
 * @author Bartosz Spyrko-Smietanko
 */
@Singleton
@Startup
public class SingletonOne {
    @PostConstruct
    public void startUp() {
        throw new RuntimeException("Singleton one fails");
    }
}
