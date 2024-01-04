/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.bridgemethods;

import jakarta.ejb.Local;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;

/**
 * @author Stuart Douglas
 */
@Stateless
@Local(ConcreteInterface.class)
public class BridgeMethodEjb implements ConcreteInterface {

    @Override
    @Interceptors(EjbInterceptor.class)
    public Integer method(final boolean intercepted) {
        return intercepted ? 1 : 0;
    }

    @Override
    @BridgeIntercepted
    public Integer cdiMethod(final boolean intercepted) {
        return intercepted ? 1 : 0;
    }
}
