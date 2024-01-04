/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.packaging.injection;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@Stateless
public class BaseBean {

    private String message;

    @PostConstruct
    public void postConstruct() {
        message = "Hello World";
    }

    public String sayHello() {
        return message;
    }

}
