/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.ejb.interceptor.packaging;

import jakarta.ejb.Stateless;

/**
 * This is not deployed in an archive with a beans.xml file
 *
 * @author Stuart Douglas
 */
@Stateless
@CdiInterceptorBinding
public class NonCdiEjb {

    public String sayHello() {
        return "Hello";
    }

}
