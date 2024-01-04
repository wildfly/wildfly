/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.mdb.cdi;

import jakarta.enterprise.context.RequestScoped;

/**
 * @author Stuart Douglas
 */
@RequestScoped
public class RequestScopedCDIBean {

    public String sayHello() {
        return CdiIntegrationMDB.REPLY;
    }

}
