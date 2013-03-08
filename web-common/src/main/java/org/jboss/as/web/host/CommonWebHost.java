package org.jboss.as.web.host;

import org.jboss.msc.service.ServiceName;

/**
 * @author Stuart Douglas
 */
public interface CommonWebHost {

    ServiceName SERVICE_NAME = ServiceName.JBOSS.append("web", "common", "host");

    CommonWebDeployment addWebDeployment(CommonWebDeploymentBuilder commonWebDeploymentBuilder) throws Exception;

}
