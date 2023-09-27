/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.lifecycle;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Stateful;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Stateful
public class Child extends Parent {
    static boolean postConstructCalled = false;

    @PostConstruct
    private void postConstruct() {
        postConstructCalled = true;
    }
}
