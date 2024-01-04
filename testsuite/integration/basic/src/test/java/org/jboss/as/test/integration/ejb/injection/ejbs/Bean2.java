/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.injection.ejbs;

import jakarta.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@Stateless(name = "bean")
public class Bean2 implements BeanInterface {
    @Override
    public String name() {
        return "Bean2";
    }
}
