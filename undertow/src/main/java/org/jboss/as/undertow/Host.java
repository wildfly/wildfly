package org.jboss.as.undertow;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.Servlet;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.form.MultiPartHandler;
import io.undertow.server.handlers.resource.FileResourceManager;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletInfo;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import org.jboss.as.web.host.ServletBuilder;
import org.jboss.as.web.host.WebDeploymentBuilder;
import org.jboss.as.web.host.WebDeploymentController;
import org.jboss.as.web.host.WebHost;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 * @author Radoslav Husar
 */
public class Host implements Service<Host>, WebHost {
    private final PathHandler pathHandler = new PathHandler();
    private final Set<String> allAliases;
    private final String name;
    private final InjectedValue<Server> server = new InjectedValue<>();
    private final InjectedValue<UndertowService> undertowService = new InjectedValue<>();
    private volatile MultiPartHandler rootHandler;

    protected Host(String name, List<String> aliases) {
        this.name = name;
        Set<String> hosts = new HashSet<>(aliases.size() + 1);
        hosts.add(name);
        hosts.addAll(aliases);
        allAliases = Collections.unmodifiableSet(hosts);
        rootHandler = new MultiPartHandler();
    }

    @Override
    public void start(StartContext context) throws StartException {
        pathHandler.setDefaultHandler(ResponseCodeHandler.HANDLE_404);
        rootHandler.setNext(pathHandler);
        server.getValue().registerHost(this);
        UndertowLogger.ROOT_LOGGER.infof("Starting host %s", name);
    }

    @Override
    public void stop(StopContext context) {
        server.getValue().unregisterHost(this);
        pathHandler.clearPaths();
        UndertowLogger.ROOT_LOGGER.infof("Stopping host %s", name);
    }

    @Override
    public Host getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    protected InjectedValue<Server> getServer() {
        return server;
    }

    protected InjectedValue<UndertowService> getUndertowService() {
        return undertowService;
    }

    public Set<String> getAllAliases() {
        return allAliases;
    }

    public String getName() {
        return name;
    }

    protected HttpHandler getRootHandler() {
        return rootHandler;
    }

    public void registerDeployment(DeploymentInfo deploymentInfo, HttpHandler handler) {
        String path = ServletContainerService.getDeployedContextPath(deploymentInfo);
        registerHandler(path, handler);
        UndertowLogger.ROOT_LOGGER.registerWebapp(path);
        undertowService.getValue().fireEvent(EventType.DEPLOYMENT_START, deploymentInfo);
    }

    public void unregisterDeployment(DeploymentInfo deploymentInfo) {
        String path = ServletContainerService.getDeployedContextPath(deploymentInfo);
        unregisterHandler(path);
        UndertowLogger.ROOT_LOGGER.unregisterWebapp(path);
        undertowService.getValue().fireEvent(EventType.DEPLOYMENT_STOP, deploymentInfo);
    }

    public void registerHandler(String path, HttpHandler handler) {
        pathHandler.addPath(path, handler);
    }

    public void unregisterHandler(String path) {
        pathHandler.removePath(path);
    }

    /**
     * @return set of registered contexts for this Host
     */
    public Set<String> getContexts() {
        return pathHandler.getPaths().keySet();
    }

    @Override
    public WebDeploymentController addWebDeployment(final WebDeploymentBuilder webDeploymentBuilder) throws Exception {

        DeploymentInfo d = new DeploymentInfo();
        d.setDeploymentName(webDeploymentBuilder.getContextRoot());
        d.setContextPath(webDeploymentBuilder.getContextRoot());
        d.setClassLoader(webDeploymentBuilder.getClassLoader());
        d.setResourceManager(new FileResourceManager(Paths.get(webDeploymentBuilder.getDocumentRoot().getAbsolutePath())));
        for (ServletBuilder servlet : webDeploymentBuilder.getServlets()) {
            ServletInfo s;
            if (servlet.getServlet() == null) {
                s = new ServletInfo(servlet.getServletName(), (Class<? extends Servlet>) servlet.getServletClass());
            } else {
                s = new ServletInfo(servlet.getServletName(), (Class<? extends Servlet>) servlet.getServletClass(), new ImmediateInstanceFactory<>(servlet.getServlet()));
            }
            if (servlet.isForceInit()){
                s.setLoadOnStartup(1);
            }
            s.addMappings(servlet.getUrlMappings());
            for (Map.Entry<String, String> param : servlet.getInitParams().entrySet()) {
                s.addInitParam(param.getKey(), param.getValue());
            }
            d.addServlet(s);
        }

        return new WebDeploymentControllerImpl(d);
    }

    private class WebDeploymentControllerImpl implements WebDeploymentController {

        private final DeploymentInfo deploymentInfo;
        private volatile DeploymentManager manager;

        private WebDeploymentControllerImpl(final DeploymentInfo deploymentInfo) {
            this.deploymentInfo = deploymentInfo;
        }

        @Override
        public void create() throws Exception {
            ServletContainer container = getServer().getValue().getServletContainer().getValue().getServletContainer();
            manager = container.addDeployment(deploymentInfo);
            manager.deploy();
        }

        @Override
        public void start() throws Exception {
            HttpHandler handler = manager.start();
            registerDeployment(deploymentInfo,handler);
        }

        @Override
        public void stop() throws Exception {
            manager.stop();
            unregisterDeployment(deploymentInfo);
        }

        @Override
        public void destroy() throws Exception {
            manager.undeploy();
            ServletContainer container = getServer().getValue().getServletContainer().getValue().getServletContainer();
            container.removeDeployment(deploymentInfo.getDeploymentName());
        }
    }

}
