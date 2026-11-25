/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.mvc.ear;

import java.util.logging.Logger;

import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/non-mvc")
@RequestScoped
public class NonMVCResource {

    private static final Logger LOGGER = Logger.getLogger(NonMVCResource.class.getName());

    @GET
    public String test() {

        LOGGER.info(getClass().getSimpleName() + "test() called");
        return "No View";
    }
}
