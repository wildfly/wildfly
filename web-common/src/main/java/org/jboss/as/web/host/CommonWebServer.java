package org.jboss.as.web.host;

import org.jboss.msc.service.ServiceName;

/**
 * @author Stuart Douglas
 */
public interface CommonWebServer {

    ServiceName SERVICE_NAME = ServiceName.JBOSS.append("web", "common", "server");

    int getPort(String protocol, boolean secure);

}
