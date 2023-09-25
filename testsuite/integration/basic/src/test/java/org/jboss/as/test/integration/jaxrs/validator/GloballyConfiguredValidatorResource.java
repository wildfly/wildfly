/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jaxrs.validator;

import jakarta.validation.constraints.Min;
import jakarta.validation.executable.ExecutableType;
import jakarta.validation.executable.ValidateOnExecution;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;

@Path("globally-configured-validate/{id}")
@Produces("text/plain")
public class GloballyConfiguredValidatorResource {

    @GET
    @Path("disabled")
    public ValidatorModel getWithoutValidation(@PathParam("id") @Min(value = 4) int id) {
        return new ValidatorModel(id);
    }

    @GET
    @Path("enabled")
    @ValidateOnExecution(type = ExecutableType.NON_GETTER_METHODS)
    public ValidatorModel getWithValidation(@PathParam("id") @Min(value = 4) int id) {
        return new ValidatorModel(id);
    }
}
