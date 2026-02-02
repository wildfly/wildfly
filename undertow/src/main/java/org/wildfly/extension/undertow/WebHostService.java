/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.undertow;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import jakarta.servlet.Servlet;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.resource.PathResourceManager;
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
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.requestcontroller.RequestController;
import org.wildfly.extension.undertow.deployment.ControlPointDeploymentInfoConfigurator;

/**
 * Implementation of WebHost from common web, service starts with few more dependencies than default Host
 *
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class WebHostService implements Service<WebHost>, WebHost {
    private final Consumer<WebHost> webHostConsumer;
    private final Supplier<Server> server;
    private final Supplier<Host> host;
    private final Supplier<RequestController> requestController;
    private volatile ControlPoint controlPoint;

    WebHostService(final Consumer<WebHost> webHostConsumer, final Supplier<Server> server,
                   final Supplier<Host> host, final Supplier<RequestController> requestController) {
        this.webHostConsumer = webHostConsumer;
        this.server = server;
        this.host = host;
        this.requestController = requestController;
    }

    @Override
    public WebDeploymentController addWebDeployment(final WebDeploymentBuilder webDeploymentBuilder) {
        DeploymentInfo d = new DeploymentInfo();

        UnaryOperator<DeploymentInfo> decorator = (this.controlPoint != null) ? new ControlPointDeploymentInfoConfigurator(this.controlPoint, webDeploymentBuilder.getAllowRequestPredicates()) : UnaryOperator.identity();

        d.setDeploymentName(webDeploymentBuilder.getContextRoot());
        d.setContextPath(webDeploymentBuilder.getContextRoot());
        d.setClassLoader(webDeploymentBuilder.getClassLoader());
        d.setResourceManager(new PathResourceManager(webDeploymentBuilder.getDocumentRoot().toPath().toAbsolutePath(), 1024L * 1024L));
        d.setIgnoreFlush(false);
        for (ServletBuilder servlet : webDeploymentBuilder.getServlets()) {
            ServletInfo s;
            if (servlet.getServlet() == null) {
                s = new ServletInfo(servlet.getServletName(), (Class<? extends Servlet>) servlet.getServletClass());
            } else {
                s = new ServletInfo(servlet.getServletName(), (Class<? extends Servlet>) servlet.getServletClass(), new ImmediateInstanceFactory<>(servlet.getServlet()));
            }
            if (servlet.isForceInit()) {
                s.setLoadOnStartup(1);
            }
            s.addMappings(servlet.getUrlMappings());
            for (Map.Entry<String, String> param : servlet.getInitParams().entrySet()) {
                s.addInitParam(param.getKey(), param.getValue());
            }
            d.addServlet(s);
        }

        return new WebDeploymentControllerImpl(decorator.apply(d));
    }

    private class WebDeploymentControllerImpl implements WebDeploymentController {

        private final DeploymentInfo deploymentInfo;
        private volatile DeploymentManager manager;

        private WebDeploymentControllerImpl(final DeploymentInfo deploymentInfo) {
            this.deploymentInfo = deploymentInfo;
        }

        @Override
        public void create() throws Exception {
            ServletContainer container = server.get().getServletContainer().getServletContainer();
            manager = container.addDeployment(deploymentInfo);
            manager.deploy();
        }

        @Override
        public void start() throws Exception {
            HttpHandler handler = manager.start();
            host.get().registerDeployment(manager.getDeployment(), handler);
        }

        @Override
        public void stop() throws Exception {
            host.get().unregisterDeployment(manager.getDeployment());
            manager.stop();
        }

        @Override
        public void destroy() throws Exception {
            manager.undeploy();
            ServletContainer container = server.get().getServletContainer().getServletContainer();
            container.removeDeployment(deploymentInfo);
        }
    }

    @Override
    public void start(final StartContext context) {
        RequestController rq = requestController != null ? requestController.get() : null;
        if (rq != null) {
            controlPoint = rq.getControlPoint("", "org.wildfly.undertow.webhost." + server.get().getName() + "." + host.get().getName());
        }
        webHostConsumer.accept(this);
    }

    @Override
    public void stop(final StopContext context) {
        webHostConsumer.accept(null);
        if (controlPoint != null) {
            requestController.get().removeControlPoint(controlPoint);
        }
    }

    @Override
    public WebHost getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}
