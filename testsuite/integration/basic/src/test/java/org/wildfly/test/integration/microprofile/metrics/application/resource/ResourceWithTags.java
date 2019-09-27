package org.wildfly.test.integration.microprofile.metrics.application.resource;

import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.Counted;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("greetings")
public class ResourceWithTags {
    @GET
    @Path("hello")
    @Counted(name = "greetings", tags = "greeting=formal")
    public String hello() {
        return "hello";
    }

    @GET
    @Path("hi")
    @Counted(name = "greetings", tags = "greeting=casual")
    public String hi() {
        return "hi";
    }

    @Inject
    MetricRegistry metricRegistry;

    @GET
    @Path("{unknownGreeting}")
    public String greet(@PathParam("unknownGreeting") String unknownGreeting) {
        Counter counter = metricRegistry.counter("UnknownGreetings", new Tag("greeting", unknownGreeting));
        counter.inc();
        return "...";
    }

}
