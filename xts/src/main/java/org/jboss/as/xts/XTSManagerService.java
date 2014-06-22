/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.xts;

import org.jboss.as.xts.logging.XtsAsLogger;
import org.jboss.jbossts.XTSService;
import org.jboss.jbossts.xts.environment.WSCEnvironmentBean;
import org.jboss.jbossts.xts.environment.XTSPropertyManager;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.wsf.spi.management.ServerConfig;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * Main XTS service
 *
 * @author <a href="mailto:adinn@redhat.com">Andrew Dinn</a>
 */
public class XTSManagerService extends AbstractService<XTSService> {
    private final String coordinatorURL;
    private volatile org.jboss.jbossts.XTSService xtsService;
    private InjectedValue<ServerConfig> wsServerConfig = new InjectedValue<ServerConfig>();

    public XTSManagerService(String coordinatorURL) {
        this.coordinatorURL = coordinatorURL;
        this.xtsService = null;
    }

    @Override
    public XTSService getValue() throws IllegalStateException {
        return xtsService;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        // XTS expects the TCCL to be set to something that will locate the XTS service implementation classes.
        final ClassLoader loader = XTSService.class.getClassLoader();
        WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(loader);
        try {
            ServerConfig serverConfigValue =  wsServerConfig.getValue();
            WSCEnvironmentBean wscEnVBean = XTSPropertyManager.getWSCEnvironmentBean();

            if (coordinatorURL !=null ) {
                wscEnVBean.setCoordinatorURL11(coordinatorURL);
            }
            else {
                //Defaults to insecure (http) on this server's bind address.
                String defaultCoordinatorUrl = "http://" + serverConfigValue.getWebServiceHost() + ":" +
                        serverConfigValue.getWebServicePort() + "/" + wscEnVBean.getCoordinatorPath11();
                wscEnVBean.setCoordinatorURL11(defaultCoordinatorUrl);
            }

            wscEnVBean.setBindAddress11(serverConfigValue.getWebServiceHost());
            wscEnVBean.setBindPort11(serverConfigValue.getWebServicePort());
            wscEnVBean.setBindPortSecure11(serverConfigValue.getWebServiceSecurePort());

            XTSService service = new XTSService();
            try {
                service.start();
            } catch (Exception e) {
                throw XtsAsLogger.ROOT_LOGGER.xtsServiceFailedToStart();
            }

            xtsService = service;
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged((ClassLoader) null);
        }
    }

    public synchronized void stop(final StopContext context) {
        if (xtsService != null) {
            try {
                xtsService.stop();
            } catch (Exception e) {
                // ignore?
            }
        }
    }

    public InjectedValue<ServerConfig> getWSServerConfig() {
        return wsServerConfig;
    }

}
