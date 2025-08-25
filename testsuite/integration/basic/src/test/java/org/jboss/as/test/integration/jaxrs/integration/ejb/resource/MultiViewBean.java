/*
 * Copyright The RESTEasy Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jaxrs.integration.ejb.resource;

import jakarta.annotation.Resource;
import jakarta.ejb.Local;
import jakarta.ejb.LocalBean;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.Path;

/**
 * Represents a view for multiple interfaces. It uses the {@link LocalBean} annotation to also create a no-interface
 * view. This is the expected behavior for Jakarta REST endpoint represented by a session bean.
 *
 * @author <a href="mailto:jperkins@ibm.com">James R. Perkins</a>
 */
@Stateless
@Local({GreetBean.class, FarewellBean.class})
@LocalBean
@Path("multiview")
public class MultiViewBean implements GreetBean, FarewellBean {

    @Inject
    private SimpleBean simpleBean;

    @Resource
    private SessionContext ctx;

    @Override
    public String greet() {
        if (ctx == null) {
            throw new InternalServerErrorException("The SessionContext was null");
        }
        return String.format("Hello, %s!", name());
    }

    @Override
    public String farewell() {
        if (ctx == null) {
            throw new InternalServerErrorException("The SessionContext was null");
        }
        return String.format("Goodbye, %s!", name());
    }

    private String name() {
        // We check if the simpleBean is null here for the case when CDI is not available. This is purely for test
        // re-use and not meant for anything outside of testing.
        final String componentName = ctx.getInvokedBusinessInterface().getName();
        return simpleBean == null ? componentName : componentName + simpleBean.name();
    }
}
