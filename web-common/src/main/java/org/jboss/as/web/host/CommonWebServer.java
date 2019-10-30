package org.jboss.as.web.host;

import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.msc.service.ServiceName;

/**
 * @author Stuart Douglas
 */
public interface CommonWebServer {

    @Deprecated
    ServiceName SERVICE_NAME = ServiceName.JBOSS.append("web", "common", "server");
    String CAPABILITY_NAME = "org.wildfly.web.common.server";

    RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(CAPABILITY_NAME, false, CommonWebServer.class)
            .setAllowMultipleRegistrations(true)
            .build();


    int getPort(String protocol, boolean secure);

}
