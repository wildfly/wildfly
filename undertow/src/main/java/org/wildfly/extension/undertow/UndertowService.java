package org.wildfly.extension.undertow;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import io.undertow.Version;
import io.undertow.servlet.api.DeploymentInfo;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 * @author Stuart Douglas
 */
public class UndertowService implements Service<UndertowService> {

    public static final ServiceName UNDERTOW = ServiceName.JBOSS.append("undertow");
    public static final ServiceName SERVLET_CONTAINER = UNDERTOW.append(Constants.SERVLET_CONTAINER);
    public static final ServiceName SERVER = UNDERTOW.append(Constants.SERVER);
    /**
     * The base name for jboss.web connector services.
     */
    public static final ServiceName AJP_LISTENER = UNDERTOW.append("ajp-listener");
    public static final ServiceName HTTP_LISTENER = UNDERTOW.append("http-listener");
    public static final ServiceName HTTPS_LISTENER = UNDERTOW.append("https-listener");
    public static final ServiceName HANDLER = UNDERTOW.append(Constants.HANDLER);
    public static final ServiceName FILTER = UNDERTOW.append(Constants.FILTER);
    public static final ServiceName ERROR_HANDLER = UNDERTOW.append(Constants.ERROR_HANDLER);
    /**
     * The base name for web deployments.
     */
    static final ServiceName WEB_DEPLOYMENT_BASE = UNDERTOW.append("deployment");
    private final String defaultContainer;
    private final String defaultServer;
    private final String defaultVirtualHost;
    private final Set<Server> registeredServers = new CopyOnWriteArraySet<>();
    private final List<UndertowEventListener> listeners = new CopyOnWriteArrayList<>();
    private volatile String instanceId;//todo this should be final and no setter should be exposed, currently mod cluster "wants it", this needs to change

    protected UndertowService(String defaultContainer, String defaultServer, String defaultVirtualHost, String instanceId) {
        this.defaultContainer = defaultContainer;
        this.defaultServer = defaultServer;
        this.defaultVirtualHost = defaultVirtualHost;
        this.instanceId = instanceId;
    }

    public static ServiceName deploymentServiceName(final String virtualHost, final String contextPath) {
        return WEB_DEPLOYMENT_BASE.append(virtualHost).append("".equals(contextPath) ? "/" : contextPath);
    }

    public static ServiceName virtualHostName(final String server, final String virtualHost) {
        return SERVER.append(server).append(Constants.HOST).append(virtualHost);
    }

    public static ServiceName locationServiceName(final String server, final String virtualHost, final String locationName) {
        return virtualHostName(server, virtualHost).append(Constants.LOCATION, locationName);
    }

    @Override
    public void start(StartContext context) throws StartException {
        UndertowLogger.ROOT_LOGGER.serverStarting(Version.getVersionString());
    }

    @Override
    public void stop(StopContext context) {
        UndertowLogger.ROOT_LOGGER.serverStopping(Version.getVersionString());
        fireEvent(EventType.SHUTDOWN, null);
    }

    @Override
    public UndertowService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    protected void registerServer(Server server) {
        registeredServers.add(server);
        fireEvent(EventType.SERVER_START, server);
    }

    protected void unregisterServer(Server server) {
        registeredServers.remove(server);
        fireEvent(EventType.SERVER_STOP, server);
    }

    public String getDefaultContainer() {
        return defaultContainer;
    }

    public String getDefaultServer() {
        return defaultServer;
    }

    public String getDefaultVirtualHost() {
        return defaultVirtualHost;
    }

    public Set<Server> getServers() {
        return Collections.unmodifiableSet(registeredServers);
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    /**
     * Registers custom Event listener to server
     *
     * @param listener event listener to register
     */
    public void registerListener(UndertowEventListener listener) {
        this.listeners.add(listener);
    }

    protected void fireEvent(EventType type, Object eventData) {
        for (UndertowEventListener listener : listeners) {
            switch (type) {
                case HOST_START:
                    listener.onHostStart((Host) eventData);
                    break;
                case HOST_STOP:
                    listener.onHostStop((Host) eventData);
                    break;
                case DEPLOYMENT_START:
                    listener.onDeploymentStart((DeploymentInfo) eventData);
                    break;
                case DEPLOYMENT_STOP:
                    listener.onDeploymentStop((DeploymentInfo) eventData);
                    break;
                case SERVER_START:
                    listener.onServerStart((Server) eventData);
                    break;
                case SERVER_STOP:
                    listener.onServerStop((Server) eventData);
                    break;
                case SHUTDOWN:
                    listener.onShutdown();
                    break;
                default:
                    throw new IllegalArgumentException("not supported yet");
            }
        }
    }
}
