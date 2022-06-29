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
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;

import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * API exposed by the Weld capability.
 *
 * @author Yeray Borges
 */
public interface WeldCapability {

    /**
     * Registers a CDI Portable Extension for the {@link DeploymentUnit} passed as argument to
     * this method.
     * <p>
     * The extension is registered if only if the DeploymentUnit is part of a Weld Deployment. Specifically,
     * if a call to {@link #isPartOfWeldDeployment(DeploymentUnit)} using the DeploymentUnit argument
     * returns {@code true}. Otherwise this method will return immediately.
     *
     * @param extension An instance of the CDI portable extension to add.
     * @param unit      The deployment unit where the extension will be registered.
     */
    void registerExtensionInstance(final Extension extension, final DeploymentUnit unit);

    /**
     * Adds the Bean Manager service associated to the {@link DeploymentUnit} to ServiceBuilder passed as argument.
     * <p>
     * The Bean Manager service is added if only if the DeploymentUnit is part of a Weld Deployment. Specifically,
     * if a call to {@link #isPartOfWeldDeployment(DeploymentUnit)} using the DeploymentUnit argument
     * returns {@code true}. Otherwise this method will return {@code null}.
     *
     * @param unit           DeploymentUnit used to get the Bean Manager service name. Cannot be null.
     * @param serviceBuilder The service builder used to add the dependency. Cannot be null.
     * @return A supplier that contains the BeanManager instance if the DeploymentUnit is part of a Weld Deployment, otherwise
     * returns {@code null}.
     * @throws IllegalStateException         if this method
     *                                       have been called after {@link ServiceBuilder#setInstance(org.jboss.msc.Service)} method.
     * @throws UnsupportedOperationException if this service builder
     *                                       wasn't created via {@link ServiceTarget#addService(ServiceName)} method.
     */
    Supplier<BeanManager> addBeanManagerService(final DeploymentUnit unit, final ServiceBuilder<?> serviceBuilder);

    /**
     * Adds the Bean Manager service associated to the {@link DeploymentUnit} to ServiceBuilder passed as argument.
     * Use this method to add the service dependency for legacy msc services.
     * <p>
     * The Bean Manager service is added if only if the DeploymentUnit is part of a Weld Deployment. Specifically,
     * if a call to {@link #isPartOfWeldDeployment(DeploymentUnit)} using the DeploymentUnit argument
     * returns {@code true}. Otherwise this method will return immediately without applying any modification to the serviceBuilder.
     *
     * @param unit           {@link DeploymentUnit} used to get the Bean Manager service name. Cannot be null.
     * @param serviceBuilder The service builder used to add the dependency. Cannot be null.
     * @param targetInjector the injector into which the dependency should be stored. Cannot be null.
     * @return the ServiceBuilder with the Bean Manager service added.
     * @throws UnsupportedOperationException if the service builder was created via
     *                                       {@link ServiceTarget#addService(ServiceName)} method.
     */
    ServiceBuilder<?> addBeanManagerService(final DeploymentUnit unit, final ServiceBuilder<?> serviceBuilder, final Injector<BeanManager> targetInjector);

    /**
     * Returns true if the {@link DeploymentUnit} is part of a weld deployment.
     *
     * @param unit {@link DeploymentUnit} to check.
     * @return true if the {@link DeploymentUnit} is part of a weld deployment.
     */
    boolean isPartOfWeldDeployment(DeploymentUnit unit);

    /**
     * Returns true if the {@link DeploymentUnit} has a beans.xml in any of it's resource roots,
     * or is a top level deployment that contains sub-deployments that are weld deployments.
     *
     * @param unit {@link DeploymentUnit} to check.
     * @return true if the {@link DeploymentUnit} has a beans.xml in any of it's resource roots,
     * or is a top level deployment that contains sub-deployments that are weld deployments.
     */
    boolean isWeldDeployment(DeploymentUnit unit);

    /**
     * Registers a deployment as a Weld deployment, even in the absence of spec-compliant configuration files or annotations. After
     * a call to this method, calls to {@code isWeldDeployment(DeploymentUnit unit)} will return true.
     */
    void markAsWeldDeployment(DeploymentUnit unit);

    /**
     * Some Maven artifacts come with a precalculated Jandex index file. This is problematic when running in EE9
     * preview mode since the index may reference {@code javax.} annotations, while Weld looks for {@code jakarta.}
     * annotations. This results in the CDI beans from such jars not being found. This allows us to bypass using the
     * cached Jandex index for the modules passed in.
     * <p>
     * <b>Note: </b> This method works out whether running in EE9 preview mode or not. If not running in EE9 preview mode
     * calling this method is a noop.
     * <p>
     * This method is deprecated, simply because once we fully move to EE9 it will more than likely have served its purpose.
     *
     * @param deploymentUnit The deployment unit to attach the ignored modules to. The implementation of this method
     *                       will associate the ignored modules with the top level deployment unit.
     * @param moduleNames The names of the modules to ignore precalculated indexes for
     * @deprecated
     */
    @Deprecated
    void ignorePrecalculatedJandexForModules(DeploymentUnit deploymentUnit, String... moduleNames);
}
