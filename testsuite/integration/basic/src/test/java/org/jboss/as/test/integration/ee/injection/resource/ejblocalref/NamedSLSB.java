/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.resource.ejblocalref;

import jakarta.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@Stateless(name = "namedBean")
public class NamedSLSB implements Hello{


    @Override
    public String sayHello() {
        return "Named Hello";
    }
}
