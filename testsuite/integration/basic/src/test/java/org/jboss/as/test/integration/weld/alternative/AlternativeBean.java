/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.alternative;

import jakarta.enterprise.inject.Alternative;

/**
 * @author Stuart Douglas
 */
@Alternative
public class AlternativeBean extends SimpleBean {

    @Override
    public String sayHello() {
        return "Hello World";
    }
}
