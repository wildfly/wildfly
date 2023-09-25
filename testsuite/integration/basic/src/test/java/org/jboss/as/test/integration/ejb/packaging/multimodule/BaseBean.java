/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.packaging.multimodule;

import jakarta.annotation.PostConstruct;

/**
 * @author Stuart Douglas
 */
public class BaseBean {

    public static boolean postConstructCalled = false;

    @PostConstruct
    public void postConstruct() {
        postConstructCalled = true;
    }

}
