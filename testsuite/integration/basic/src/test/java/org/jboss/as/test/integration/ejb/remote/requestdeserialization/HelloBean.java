package org.jboss.as.test.integration.ejb.remote.requestdeserialization;

import javax.ejb.Stateless;

@Stateless
public class HelloBean implements HelloRemote {
    @Override
    public Response sayHello(Request request) {
        // relay the TCCL that was set during request unmarshalling
        return new Response(request.getGreeting(), request.getTccl().toString());
    }
}
