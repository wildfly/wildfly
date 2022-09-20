package org.jboss.as.test.integration.ws.basic;

import jakarta.jws.WebService;

@WebService
public interface InstanceCountEndpointIface {

    int getInstanceCount();
    String test(String payload);
}
