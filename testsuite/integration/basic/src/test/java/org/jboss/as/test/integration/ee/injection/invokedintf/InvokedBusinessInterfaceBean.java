/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.invokedintf;

import jakarta.annotation.Resource;
import jakarta.ejb.Remote;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

/**
 * @author <a href="mailto:carlo.dewolf@jboss.com">Carlo de Wolf</a>
 */
@Stateless
@Remote({ Remote1.class, Remote2.class })
public class InvokedBusinessInterfaceBean implements CommonRemote {
    @Resource
    private SessionContext ctx;

    public Class<?> getInvokedBusinessInterface() {
        return ctx.getInvokedBusinessInterface();
    }
}
