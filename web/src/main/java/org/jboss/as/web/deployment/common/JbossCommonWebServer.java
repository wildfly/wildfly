package org.jboss.as.web.deployment.common;

import org.apache.catalina.connector.Connector;
import org.jboss.as.web.WebServer;
import org.jboss.as.web.host.CommonWebServer;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Stuart Douglas
 */
public class JbossCommonWebServer implements CommonWebServer, Service<CommonWebServer> {

    private final InjectedValue<WebServer> webServer = new InjectedValue<>();


    @Override
    public int getPort(final String protocol, final boolean secure) {
        int result = -1;
        for (Connector connector : webServer.getValue().getService().findConnectors()) {
            if (connector.getProtocol().equals(protocol) && connector.getSecure() == secure) {
                return connector.getPort();
            }
        }
        return result;
    }

    public InjectedValue<WebServer> getWebServer() {
        return webServer;
    }

    @Override
    public void start(final StartContext context) throws StartException {

    }

    @Override
    public void stop(final StopContext context) {

    }

    @Override
    public CommonWebServer getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}
