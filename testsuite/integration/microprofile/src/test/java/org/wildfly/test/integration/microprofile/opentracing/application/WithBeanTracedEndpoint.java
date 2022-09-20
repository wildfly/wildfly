package org.wildfly.test.integration.microprofile.opentracing.application;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

@Path("with-bean")
@Produces({"text/plain"})
public class WithBeanTracedEndpoint {

    @Inject
    TracedBean tracedBean;

    @GET
    public String withBean() {
        tracedBean.doSomething();
        tracedBean.doSomethingElse();
        tracedBean.doYetAnotherSomething();
        return "with-bean-called";
    }
}
