/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.integration.ejb;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.interceptor.Interceptors;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("ejbInterceptor")
@Produces({"text/plain"})
@Stateless(name = "CustomName")
@Interceptors(EjbInterceptor.class)
public class EJBResource implements EjbInterface {

    @Resource
    private TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    @GET
    public String getMessage() throws SystemException {
        if(transactionSynchronizationRegistry.getTransactionStatus() != Status.STATUS_ACTIVE) {
            throw new RuntimeException("Transaction not active, not an EJB invocation");
        }
        return "Hello";
    }
}
