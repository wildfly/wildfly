/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
