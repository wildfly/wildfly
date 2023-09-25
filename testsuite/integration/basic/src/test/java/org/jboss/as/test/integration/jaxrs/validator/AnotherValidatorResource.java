/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.validator;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

@Path("another-validate/{id}")
@Produces("text/plain")
public class AnotherValidatorResource {

    @PathParam("id")
    @Min(value = 4)
    private int id;

    @Valid
    @GET
    public ValidatorModel getValidatorModel() {
        return new ValidatorModel(id);
    }
}
