/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.xts;

import org.jboss.as.xts.logging.XtsAsLogger;
import org.jboss.jbossts.XTSService;
import org.jboss.jbossts.xts.environment.WSCEnvironmentBean;
import org.jboss.jbossts.xts.environment.XTSPropertyManager;
import org.jboss.msc.service.Service;
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
public class XTSManagerService implements Service<XTSService> {
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
