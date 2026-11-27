/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.extensions;

import jakarta.ejb.Local;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;

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
        Assertions.assertNotNull(myBean);
    }
}
