/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.jaxrs;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * WS server config service.
 *
 * @author <a href="alessio.soldano@jboss.com">Alessio Soldano</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 * @author <a href="mailto:rsigal@redhat.com">Ron Sigal</a>
 */
public final class JaxrsServerConfigService implements Service<JaxrsServerConfig> {

    public static final ServiceName JAXRS_SERVICE = ServiceName.JBOSS.append("jaxrs");
    public static final ServiceName CONFIG_SERVICE = JAXRS_SERVICE.append("config");

    private final JaxrsServerConfig serverConfig;

    private JaxrsServerConfigService(final JaxrsServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @Override
    public void start(final StartContext context) throws StartException {
    }

    @Override
    public void stop(final StopContext context) {
    }

    public static ServiceController<?> install(final ServiceTarget serviceTarget, final JaxrsServerConfig serverConfig) {
        final ServiceBuilder<?> builder = serviceTarget.addService(CONFIG_SERVICE);
        builder.setInstance(new JaxrsServerConfigService(serverConfig));
        return builder.install();
    }

    public JaxrsServerConfig getJaxrsServerConfig() {
        return serverConfig;
    }

    @Override
    public JaxrsServerConfig getValue() throws IllegalStateException, IllegalArgumentException {
        return serverConfig;
    }
}
