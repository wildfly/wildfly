/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.mvc.ear;

import java.util.logging.Logger;

import jakarta.enterprise.context.RequestScoped;
import jakarta.mvc.Controller;
import jakarta.mvc.View;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/shared")
@Controller
@RequestScoped
public class SharedMVCController {

    private static final Logger LOGGER = Logger.getLogger(SharedMVCController.class.getName());

    @GET
    @View("view.jsp")
    public void test() {
        LOGGER.info(getClass().getSimpleName() + "test() called");
    }

}
