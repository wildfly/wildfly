/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.annotationprocessing;

import jakarta.ejb.Stateless;

@Stateless
public class ReferencedBean implements ReferencedBeanInterface {
    @Override
    public String sayHello() {
        return "Hello!";
    }
}
