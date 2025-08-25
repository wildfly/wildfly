/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.integration.ejb.resource;

import jakarta.annotation.Resource;
import jakarta.ejb.Local;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.Path;

/**
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@Stateless
@Local(GreetBean.class)
@Path("greet")
public class GreetClientView implements GreetBean {

    @Resource
    private SessionContext ctx;

    @Override
    public String greet() {
        if (ctx == null) {
            throw new InternalServerErrorException("The SessionContext was null");
        }
        return String.format("Hello, %s!", ctx.getInvokedBusinessInterface().getName());
    }
}
