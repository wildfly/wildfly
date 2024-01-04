/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.inheritance;

import jakarta.ejb.Stateless;

/**
 * @author Stuart Douglas
 */
@Stateless
public class InterfaceBean implements ChildInterface{
    @Override
    public String interfaceMethod() {
        return "Interface";
    }
}
