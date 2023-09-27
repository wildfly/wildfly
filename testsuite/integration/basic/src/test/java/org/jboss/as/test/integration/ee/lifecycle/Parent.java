/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.lifecycle;

import jakarta.annotation.PostConstruct;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class Parent {
    static boolean postConstructCalled = false;

    @PostConstruct
    private void postConstruct() {
        postConstructCalled = true;
    }
}
