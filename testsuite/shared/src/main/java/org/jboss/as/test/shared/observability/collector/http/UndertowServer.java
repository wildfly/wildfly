/*
 *
 *  * Copyright The WildFly Authors
 *  * SPDX-License-Identifier: Apache-2.0
 *
 */

package org.jboss.as.test.shared.observability.collector.http;

import java.util.function.Consumer;

import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.servlet.Servlets;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.util.ImmediateInstanceFactory;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import org.jboss.as.test.shared.observability.signals.SimpleMetric;
import org.jboss.as.test.shared.observability.signals.trace.SimpleSpan;

public class UndertowServer {
    protected Consumer<SimpleSpan> traceConsumer;
    protected Consumer<SimpleMetric> metricsConsumer;
    protected Undertow server;

    public UndertowServer(Consumer<SimpleSpan> traceConsumer, Consumer<SimpleMetric> metricsConsumer) {
        this.traceConsumer = traceConsumer;
        this.metricsConsumer = metricsConsumer;
    }

    public void start() throws ServletException {

        DeploymentInfo deployment = Servlets.deployment()
                .setClassLoader(UndertowServer.class.getClassLoader())
                .setContextPath("/")
                .setDeploymentName("traces.war")
                // TODO: Add logs support for OTLP over HTTP if needed
                .addServlet(Servlets.servlet("MetricsServlet", MetricsServlet.class,
                                new ImmediateInstanceFactory<Servlet>(new MetricsServlet(metricsConsumer)))
                        .addMapping("/v1/metrics"))
                .addServlet(Servlets.servlet("TracesServlet", TracesServlet.class,
                                new ImmediateInstanceFactory<Servlet>(new TracesServlet(traceConsumer)))
                        .addMapping("/v1/traces"));

        DeploymentManager manager = Servlets.defaultContainer().addDeployment(deployment);
        manager.deploy();

        server = Undertow.builder()
                .setHandler(manager.start())
                .setIoThreads(Runtime.getRuntime().availableProcessors())
                .setWorkerThreads(64)
                .setServerOption(UndertowOptions.MAX_ENTITY_SIZE, 10 * 1024 * 1024L)
                .addHttpListener(4318,
                        System.getProperty("ipv6") != null ? "::" : "0.0.0.0")
                .build();

        server.start();
    }

    public void shutdown() {
        server.stop();
    }
}
