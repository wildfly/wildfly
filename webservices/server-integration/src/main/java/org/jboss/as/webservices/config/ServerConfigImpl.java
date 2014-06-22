/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.webservices.config;

import java.io.File;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;

import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.msc.value.InjectedValue;
import org.jboss.ws.common.management.AbstractServerConfig;
import org.jboss.ws.common.management.AbstractServerConfigMBean;
import org.jboss.wsf.spi.metadata.config.ClientConfig;

/**
 * WFLY specific ServerConfig, extending AbstractServerConfig with management
 * related functionalities.
 *
 * @author <a href="mailto:asoldano@redhat.com">Alessio Soldano</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:tdiesler@redhat.com">Thomas Diesler</a>
 */
public final class ServerConfigImpl extends AbstractServerConfig implements AbstractServerConfigMBean {

    private final InjectedValue<MBeanServer> injectedMBeanServer = new InjectedValue<MBeanServer>();
    private final InjectedValue<ServerEnvironment> injectedServerEnvironment = new InjectedValue<ServerEnvironment>();
    private final AtomicInteger wsDeploymentCount = new AtomicInteger(0);

    private final DMRSynchCheckHandler webServiceHostUCH = new DMRSynchCheckHandler();
    private final DMRSynchCheckHandler webServicePortUCH = new DMRSynchCheckHandler();
    private final DMRSynchCheckHandler webServiceSecurePortUCH = new DMRSynchCheckHandler();
    private final DMRSynchCheckHandler modifySOAPAddressUCH = new DMRSynchCheckHandler();

    private ServerConfigImpl() {
        // forbidden inheritance
    }

    @Override
    public void create() throws Exception {
        super.create();
        wsDeploymentCount.set(0);
        webServiceHostUCH.reset();
        webServicePortUCH.reset();
        webServiceSecurePortUCH.reset();
        modifySOAPAddressUCH.reset();
    }

    public void incrementWSDeploymentCount() {
        wsDeploymentCount.incrementAndGet();
    }

    public void decrementWSDeploymentCount() {
        wsDeploymentCount.decrementAndGet();
    }

    protected boolean isModifiable() {
        return (wsDeploymentCount.get() == 0);
    }

    public void setWebServiceHost(String host, boolean forceUpdate) throws UnknownHostException {
        setWebServiceHost(host, forceUpdate ? null : webServiceHostUCH);
    }

    @Override
    public void setWebServiceHost(String host) throws UnknownHostException {
        //prevent any change if the DMR configuration is not in synch anymore with the runtime
        setWebServiceHost(host, webServiceHostUCH);
    }

    public void setWebServicePort(int port, boolean forceUpdate) {
        setWebServicePort(port, forceUpdate ? null : webServicePortUCH);
    }

    @Override
    public void setWebServicePort(int port) {
        //prevent any change if the DMR configuration is not in synch anymore with the runtime
        setWebServicePort(port, webServicePortUCH);
    }

    public void setWebServiceSecurePort(int port, boolean forceUpdate) {
        setWebServiceSecurePort(port, forceUpdate ? null : webServiceSecurePortUCH);
    }

    @Override
    public void setWebServiceSecurePort(int port) {
        //prevent any change if the DMR configuration is not in synch anymore with the runtime
        setWebServiceSecurePort(port, webServiceSecurePortUCH);
    }

    public void setModifySOAPAddress(boolean flag, boolean forceUpdate) {
        setModifySOAPAddress(flag, forceUpdate ? null : modifySOAPAddressUCH);
    }

    @Override
    public void setModifySOAPAddress(boolean flag) {
        //prevent any change if the DMR configuration is not in synch anymore with the runtime
        setModifySOAPAddress(flag, modifySOAPAddressUCH);
    }


    public File getServerTempDir() {
        return getServerEnvironment().getServerTempDir();
    }

    public File getHomeDir() {
        return getServerEnvironment().getHomeDir();
    }

    public File getServerDataDir() {
        return getServerEnvironment().getServerDataDir();
    }

    @Override
    public MBeanServer getMbeanServer() {
        return injectedMBeanServer.getValue();
    }

    @Override
    public void setMbeanServer(final MBeanServer mbeanServer) {
        throw new UnsupportedOperationException();
    }

    public InjectedValue<MBeanServer> getMBeanServerInjector() {
        return injectedMBeanServer;
    }

    public InjectedValue<ServerEnvironment> getServerEnvironmentInjector() {
        return injectedServerEnvironment;
    }

    private ServerEnvironment getServerEnvironment() {
        return injectedServerEnvironment.getValue();
    }

    public static ServerConfigImpl newInstance() {
        return new ServerConfigImpl();
    }

    public void setClientConfigWrapper(ClientConfig config, boolean reload) {
        clientConfigStore.setWrapperConfig(config, reload);
    }

    private class DMRSynchCheckHandler implements UpdateCallbackHandler {

        private volatile boolean dmrSynched = true;

        @Override
        public void onBeforeUpdate() {
            if (!dmrSynched) {
                throw WSLogger.ROOT_LOGGER.couldNotUpdateServerConfigBecauseOfReloadRequired();
            }
            //prevent any modification to the AbstractServerConfig members
            //when there's at least a WS endpoint deployment on the server
            if (!isModifiable()) {
                dmrSynched = false;
                throw WSLogger.ROOT_LOGGER.couldNotUpdateServerConfigBecauseOfExistingWSDeployment();
            }
        }

        public void reset() {
            dmrSynched = true;
        }
    }
}
