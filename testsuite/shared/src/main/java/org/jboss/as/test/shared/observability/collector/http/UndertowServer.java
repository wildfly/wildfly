/*
 *
 *  * Copyright The WildFly Authors
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.as.test.shared.observability.collector.http;

import java.util.function.Consumer;

import io.undertow.Undertow;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import org.jboss.as.test.shared.observability.signals.SimpleMetric;
import org.jboss.as.test.shared.observability.signals.trace.Span;

public class UndertowServer {
    protected Consumer<Span> traceConsumer;
    protected Consumer<SimpleMetric> metricsConsumer;
    protected Undertow server;

    public UndertowServer(Consumer<Span> traceConsumer, Consumer<SimpleMetric> metricsConsumer) {
        this.traceConsumer = traceConsumer;
        this.metricsConsumer = metricsConsumer;
    }

    public void start() throws ServletException {

        DeploymentInfo deployment = Servlets.deployment()
                .setClassLoader(UndertowServer.class.getClassLoader())
                .setContextPath("/")
                .setDeploymentName("traces.war")
                .addServlet(Servlets.servlet("MetricsServlet", MetricsServlet.class,
                                new ImmediateInstanceFactory<Servlet>(new MetricsServlet(metricsConsumer)))
                        .addMapping("/v1/metrics"))
                .addServlet(Servlets.servlet("TracesServlet", TracesServlet.class,
                                new ImmediateInstanceFactory<Servlet>(new TracesServlet(traceConsumer)))
                        .addMapping("/v1/traces"))
                ;

        DeploymentManager manager = Servlets.defaultContainer()
                .addDeployment(deployment);

        manager.deploy();

        server = Undertow.builder()
                .addHttpListener(4318, "0.0.0.0")
                .setHandler(manager.start())
                .build();

        server.start();

        System.out.println("Undertow listening on http://localhost:4318/v1/traces");
    }

    public void shutdown() {
        server.stop();
    }
}
