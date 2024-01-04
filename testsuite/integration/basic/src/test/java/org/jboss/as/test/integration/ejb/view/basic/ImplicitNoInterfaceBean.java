/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.view.basic;

import jakarta.ejb.Stateless;

/**
 * User: jpai
 */
@Stateless
public class ImplicitNoInterfaceBean extends NoInterfaceSuperclass {

    @Override
    public String sayHello() {
        return super.sayHello();
    }
}
