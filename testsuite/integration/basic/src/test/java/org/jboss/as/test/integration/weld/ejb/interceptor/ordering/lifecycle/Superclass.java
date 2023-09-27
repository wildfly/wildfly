/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.interceptor.ordering.lifecycle;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

public class Superclass {

    @PostConstruct
    void postConstructSuperclass() {
        ActionSequence.addAction(Superclass.class.getSimpleName());
    }

    @PreDestroy
    void preDestroySuperclass() {
        ActionSequence.addAction(Superclass.class.getSimpleName());
    }
}
