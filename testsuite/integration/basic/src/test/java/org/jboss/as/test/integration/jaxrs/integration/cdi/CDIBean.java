/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.integration.cdi;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * @author Stuart Douglas
 */
@ApplicationScoped
public class CDIBean {

    public String message() {
        return "Hello World!";
    }

}
