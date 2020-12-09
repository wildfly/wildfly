package org.jboss.as.test.manualmode.server.graceless.deploymentb;

import java.util.concurrent.CompletionStage;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

@ApplicationScoped
@Path("/resourceb")
public class ResourceB {
    public void postConstruct(@Observes @Initialized(ApplicationScoped.class) Object o) {
        try {
            System.out.println("***** [graceless] CDI is ready.");
            Client client = ClientBuilder.newClient();
            CompletionStage<Response> response = client.target("http://localhost:8080/deploymenta/testa/resourcea")
                    .request()
                    .rx()
                    .get()
                    .exceptionally((e) -> {
                        e.printStackTrace();
                        return null;
                    })
                    .whenComplete((r,e) -> {
                        System.out.println("***** [graceless] Request completed.");
                        System.out.println("***** [graceless] Response status is " + r.getStatus());
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @GET
    public String get() {
        System.out.println("***** [graceless] Inside ResourceB");
        return "Hello";
    }
}