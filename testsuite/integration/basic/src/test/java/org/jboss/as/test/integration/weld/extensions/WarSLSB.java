/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.extensions;

import org.junit.Assert;

import jakarta.ejb.Local;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

/**
 * @author Stuart Douglas
 */
@Stateless
@Local(SomeInterface.class)
public class WarSLSB  implements  SomeInterface {

    @Inject
    private MyBean myBean;


    @Override
    public void testInjectionWorked() {
        Assert.assertNotNull(myBean);
    }
}
