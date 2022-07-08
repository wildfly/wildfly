package org.wildfly.test.integration.microprofile.opentracing.application;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("with-custom-operation-name")
@Produces({"text/plain"})
public class WithCustomOperationNameEndpoint {
    @Inject
    CustomOperationNameBean customOperationNameBean;

    @GET
    public String get() {
        customOperationNameBean.doSomething();
        customOperationNameBean.doSomethingElse();
        return "with-custom-operation-name-called";
    }
}
