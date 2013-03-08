package org.jboss.as.web.host;

import org.jboss.msc.service.ServiceName;

/**
 * @author Stuart Douglas
 */
public interface WebHost {

    ServiceName SERVICE_NAME = ServiceName.JBOSS.append("web", "common", "host");

    WebDeploymentController addWebDeployment(WebDeploymentBuilder webDeploymentBuilder) throws Exception;

}
