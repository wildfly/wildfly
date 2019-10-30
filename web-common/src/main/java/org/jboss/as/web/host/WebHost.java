package org.jboss.as.web.host;

import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.msc.service.ServiceName;

/**
 * @author Stuart Douglas
 */
public interface WebHost {
    @Deprecated
    ServiceName SERVICE_NAME = ServiceName.JBOSS.append("web", "common", "host");
    String CAPABILITY_NAME = "org.wildfly.web.common.host";
    RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(CAPABILITY_NAME, true, WebHost.class)
            .addRequirements(CommonWebServer.CAPABILITY_NAME)
            .setAllowMultipleRegistrations(true)
            .build();


    WebDeploymentController addWebDeployment(WebDeploymentBuilder webDeploymentBuilder) throws Exception;

}
