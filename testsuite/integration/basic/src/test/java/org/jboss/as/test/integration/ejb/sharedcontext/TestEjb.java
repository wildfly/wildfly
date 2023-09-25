/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.sharedcontext;

import jakarta.ejb.Remote;
import jakarta.ejb.Stateless;

@Remote(TestEjbRemote.class)
@Stateless
public class TestEjb implements TestEjbRemote {

    @Override
    public void test() {}
}
