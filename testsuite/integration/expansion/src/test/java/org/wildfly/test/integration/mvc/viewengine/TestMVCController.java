/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.mvc.viewengine;

import java.util.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.mvc.Controller;
import jakarta.mvc.View;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/test")
@Controller
@ApplicationScoped
public class TestMVCController {

    private static final Logger LOGGER = Logger.getLogger(TestMVCController.class.getName());

    @GET
    @View("test.ve")
    public void test() {
        LOGGER.info(getClass().getSimpleName() + "test() called");
    }
}
