package org.jboss.as.test.manualmode.server.nongraceful.deploymentb;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.jboss.as.test.shared.TimeoutUtil;

@ApplicationScoped
@Path("/resourceb")
public class ResourceB {
    public void postConstruct(@Observes @Initialized(ApplicationScoped.class) Object o) throws InterruptedException {
        final Client client = ClientBuilder.newClient();
        final int sleepTime = 100;
        int tries = 0;
        int maxLoopCount = TimeoutUtil.adjust(4500) / sleepTime;
        boolean complete = false;

        while (!complete && tries < maxLoopCount) {
            Response response = client.target("http://localhost:8080/deploymenta/testa/resourcea")
                    .request()
                    .get();
            int status = response.getStatus();
            if (status != 200) {
                tries++;
                Thread.sleep(sleepTime);
            } else {
                complete = true;
            }
        }
    }

    @GET
    public String get() {
        return "Hello";
    }
}
