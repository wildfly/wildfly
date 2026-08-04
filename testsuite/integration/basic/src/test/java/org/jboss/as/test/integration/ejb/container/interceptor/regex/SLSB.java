/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.container.interceptor.regex;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

@Stateless(name = "TestRemote")
@Remote(SLSBRemote.class)
public class SLSB implements SLSBRemote {

    @Override
    public void foo() {
    }
}
