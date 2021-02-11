package org.wildfly.test.integration.microprofile.opentracing.application;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

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
