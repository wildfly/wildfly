/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jaxrs.rsources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("fromString")
public class SimpleFromStringResource {
    @GET
    public String get(@DefaultValue("defaulFromString")
                      @QueryParam("newString") SimpleFromStringProvider p) {
        return "done";
    }
}
