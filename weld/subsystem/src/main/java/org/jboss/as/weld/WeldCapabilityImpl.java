/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.weld._private.WeldDeploymentMarker;
import org.jboss.as.weld.deployment.WeldPortableExtensions;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;

/**
 * The implementation of WeldCapability.
 *
 * @author Yeray Borges
 */
public class WeldCapabilityImpl implements WeldCapability {
    static final WeldCapability INSTANCE = new WeldCapabilityImpl();
    private final Map<String, Boolean> annotations;

    private WeldCapabilityImpl() {
        annotations = new ConcurrentHashMap<>();
    }

    @Override
    public void registerExtensionInstance(final Extension extension, final DeploymentUnit unit) {
        if (isPartOfWeldDeployment(unit)) {
            WeldPortableExtensions extensions = WeldPortableExtensions.getPortableExtensions(unit);
            extensions.registerExtensionInstance(extension, unit);
        }
    }

    @Override
    public Supplier<BeanManager> addBeanManagerService(final DeploymentUnit unit, final ServiceBuilder<?> serviceBuilder) {
        if (isPartOfWeldDeployment(unit)) {
            return serviceBuilder.requires(ServiceNames.beanManagerServiceName(unit));
        }

        return null;
    }

    @Override
    public ServiceBuilder<?> addBeanManagerService(final DeploymentUnit unit, final ServiceBuilder<?> serviceBuilder, final Injector<BeanManager> targetInjector) {
        if (isPartOfWeldDeployment(unit)) {
            return serviceBuilder.addDependency(ServiceNames.beanManagerServiceName(unit), BeanManager.class, targetInjector);
        }

        return serviceBuilder;
    }
    @Override
    public WeldCapability addBeanDefiningAnnotation(final Class<? extends Annotation> annotation, final boolean inherited) {
        annotations.put(annotation.getName(), inherited);
        return this;
    }

    @Override
    public Map<String, Boolean> getBeanDefiningAnnotations() {
        return Map.copyOf(annotations);
    }

    public boolean isPartOfWeldDeployment(final DeploymentUnit unit) {
        return WeldDeploymentMarker.isPartOfWeldDeployment(unit);
    }

    public boolean isWeldDeployment(final DeploymentUnit unit) {
        return WeldDeploymentMarker.isWeldDeployment(unit);
    }

    public void markAsWeldDeployment(DeploymentUnit unit) {
        WeldDeploymentMarker.mark(unit);
    }

}
