/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jsf.managedbean.managedproperty;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

/**
 * A greeting bean.
 *
 * @author Farah Juma
 */
@Named("greetingBean")
@ApplicationScoped
public class GreetingBean {

    public String greet(String name) {
        return "Hello " + name;
    }
}
