/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jsf.unsupportedwar;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

@Named(value = "helloBean")
@RequestScoped
public class HelloBean {

    private String name;

    public HelloBean() {
        this("World");
    }

    public HelloBean(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
