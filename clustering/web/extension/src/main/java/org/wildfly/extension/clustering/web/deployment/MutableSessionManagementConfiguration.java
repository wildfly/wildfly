/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.deployment;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.wildfly.clustering.marshalling.ByteBufferMarshaller;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.session.SessionAttributePersistenceStrategy;
import org.wildfly.clustering.web.service.routing.RouteLocatorProvider;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementConfiguration;
import org.wildfly.extension.clustering.web.SessionGranularity;
import org.wildfly.extension.clustering.web.SessionMarshallerFactory;

/**
 * @author Paul Ferraro
 */
public abstract class MutableSessionManagementConfiguration implements DistributableSessionManagementConfiguration<DeploymentUnit>, UnaryOperator<String>, Consumer<String>, BinaryServiceConfiguration {

    private final UnaryOperator<String> replacer;

    private SessionGranularity granularity;
    private Function<DeploymentUnit, ByteBufferMarshaller> marshallerFactory = SessionMarshallerFactory.JBOSS;
    private RouteLocatorProvider routeLocatorProvider;
    private final Consumer<String> accumulator;

    MutableSessionManagementConfiguration(UnaryOperator<String> replacer, Consumer<String> accumulator, RouteLocatorProvider defaultRouteLocatorProvider) {
        this.replacer = replacer;
        this.accumulator = accumulator;
        this.routeLocatorProvider = defaultRouteLocatorProvider;
    }

    @Override
    public SessionAttributePersistenceStrategy getAttributePersistenceStrategy() {
        return (this.granularity != null) ? this.granularity.getAttributePersistenceStrategy() : null;
    }

    @Override
    public Function<DeploymentUnit, ByteBufferMarshaller> getMarshallerFactory() {
        return this.marshallerFactory;
    }

    public RouteLocatorProvider getRouteLocatorProvider() {
        return this.routeLocatorProvider;
    }

    public void setSessionGranularity(String value) {
        this.granularity = SessionGranularity.valueOf(this.replacer.apply(value));
    }

    public void setMarshallerFactory(String value) {
        this.marshallerFactory = SessionMarshallerFactory.valueOf(this.replacer.apply(value));
    }

    public void setRouteLocatorProvider(RouteLocatorProvider provider) {
        this.routeLocatorProvider = provider;
    }

    @Override
    public String apply(String value) {
        return this.replacer.apply(value);
    }

    @Override
    public void accept(String value) {
        this.accumulator.accept(value);
    }
}
