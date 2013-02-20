package org.jboss.as.undertow;

import org.jboss.as.web.host.CommonWebServer;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 *
 * TODO: this is a hack for now
 * @author Stuart Douglas
 */
@Deprecated
public class WebServerService implements CommonWebServer, Service<WebServerService> {


    @Override
    public int getPort(final String protocol, final boolean secure) {
        return secure ? 8443 : 8080;
    }

    @Override
    public void start(final StartContext context) throws StartException {

    }

    @Override
    public void stop(final StopContext context) {

    }

    @Override
    public WebServerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}
