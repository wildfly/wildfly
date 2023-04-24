/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.weld;

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

    private WeldCapabilityImpl() {}

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
