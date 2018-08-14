package org.jboss.as.test.integration.microprofile.opentracing.application;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

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
