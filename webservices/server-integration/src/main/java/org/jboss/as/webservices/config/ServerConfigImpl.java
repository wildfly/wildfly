/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.config;

import java.io.File;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;

import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.ws.common.management.AbstractServerConfig;
import org.jboss.ws.common.management.AbstractServerConfigMBean;
import org.jboss.wsf.spi.metadata.config.ClientConfig;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.Server;
import org.wildfly.extension.undertow.UndertowListener;
import org.wildfly.extension.undertow.UndertowService;

/**
 * WFLY specific ServerConfig, extending AbstractServerConfig with management
 * related functionalities.
 *
 * @author <a href="mailto:asoldano@redhat.com">Alessio Soldano</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:tdiesler@redhat.com">Thomas Diesler</a>
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 */
public final class ServerConfigImpl extends AbstractServerConfig implements AbstractServerConfigMBean {

    private volatile MBeanServer mBeanServer;
    private volatile ServerEnvironment serverEnvironment;
    private volatile UndertowService undertowService;
    private final AtomicInteger wsDeploymentCount = new AtomicInteger(0);

    private final DMRSynchCheckHandler webServiceHostUCH = new DMRSynchCheckHandler();
    private final DMRSynchCheckHandler webServicePortUCH = new DMRSynchCheckHandler();
    private final DMRSynchCheckHandler webServiceSecurePortUCH = new DMRSynchCheckHandler();
    private final DMRSynchCheckHandler webServiceUriSchemeUCH = new DMRSynchCheckHandler();
    private final DMRSynchCheckHandler modifySOAPAddressUCH = new DMRSynchCheckHandler();
    private final DMRSynchCheckHandler webServicePathRewriteRuleUCH = new DMRSynchCheckHandler();

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
        webServicePathRewriteRuleUCH.reset();
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

    public void setWebServicePathRewriteRule(String path, boolean forceUpdate) {
        setWebServicePathRewriteRule(path, forceUpdate ? null : webServicePathRewriteRuleUCH);
    }

    @Override
    public void setWebServicePathRewriteRule(String path) {
        setWebServicePathRewriteRule(path, webServicePathRewriteRuleUCH);
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

    public void setWebServiceUriScheme(String scheme, boolean forceUpdate) {
        setWebServiceUriScheme(scheme, forceUpdate ? null : webServiceUriSchemeUCH);
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
        return mBeanServer;
    }

    @Override
    public void setMbeanServer(final MBeanServer mBeanServer) {
        this.mBeanServer = mBeanServer;
    }

    public void setServerEnvironment(final ServerEnvironment serverEnvironment) {
        this.serverEnvironment = serverEnvironment;
    }
    public void setUndertowService(final UndertowService undertowService) {
        this.undertowService = undertowService;
    }

    private ServerEnvironment getServerEnvironment() {
        return serverEnvironment;
    }
    private UndertowService getUndertowService() {
        return undertowService;
    }

    public static ServerConfigImpl newInstance() {
        return new ServerConfigImpl();
    }

    public void setClientConfigWrapper(ClientConfig config, boolean reload) {
        clientConfigStore.setWrapperConfig(config, reload);
    }
    @Override
    public Integer getVirtualHostPort(String hostname, boolean securePort) {
        ServerHostInfo hostInfo = new ServerHostInfo(hostname);
        Host undertowHost = getUndertowHost(hostInfo);
        if (undertowHost != null && !undertowHost.getServer().getListeners().isEmpty()) {
            for(UndertowListener listener : undertowHost.getServer().getListeners()) {
                if (listener.isSecure() == securePort) {
                    return listener.getSocketBinding().getAbsolutePort();
                }
            }
        }
        return null;
    }

    @Override
    public String getHostAlias(String hostname) {
        ServerHostInfo hostInfo = new ServerHostInfo(hostname);
        Host undertowHost = getUndertowHost(hostInfo);
        if (undertowHost!= null && !undertowHost.getAllAliases().isEmpty()) {
            for (String alias : undertowHost.getAllAliases()) {
                if (undertowHost.getAllAliases().size() == 1 || !alias.equals(undertowHost.getName())) {
                    return alias;
                }
            }
        }
        return null;
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

    private Host getUndertowHost(final ServerHostInfo info) {
        UndertowService us = getUndertowService();
        if (us != null) {
            for (Server server : getUndertowService().getServers()) {
                if (info.getServerInstanceName() != null && !server.getName().equals(info.getServerInstanceName())) {
                    continue;
                }
                for (Host undertowHost : server.getHosts()) {
                    if (undertowHost.getName().equals(info.getHost())) {
                        return undertowHost;
                    }
                }
            }
        }
        return null;
    }
}
