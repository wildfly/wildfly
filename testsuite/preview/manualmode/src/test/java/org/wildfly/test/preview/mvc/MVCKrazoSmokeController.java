/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.preview.mvc;

import jakarta.inject.Inject;
import jakarta.mvc.Controller;
import jakarta.mvc.Models;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Controller
@Path("test")
public class MVCKrazoSmokeController {

    @Inject
    private Models models;

    @Inject
    private CdiBean cdiBean;

    @GET
    public String render() {

        cdiBean.setFirstName("Joan");
        models.put("lastName", "Jett");
        return "view.jsp";
    }
}
