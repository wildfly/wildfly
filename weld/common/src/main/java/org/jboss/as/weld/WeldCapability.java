/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.weld;

import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.enterprise.inject.build.compatible.spi.BuildCompatibleExtension;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;

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
     * The extension is registered if and only if the DeploymentUnit is part of a Weld Deployment. Specifically,
     * if a call to {@link #isPartOfWeldDeployment(DeploymentUnit)} using the DeploymentUnit argument
     * returns {@code true}. Otherwise, this method will return immediately.
     *
     * @param extension An instance of the CDI portable extension to add.
     * @param unit      The deployment unit where the extension will be registered.
     */
    void registerExtensionInstance(final Extension extension, final DeploymentUnit unit);

    /**
     * Registers a CDI Build Compatible Extension for the {@link DeploymentUnit} passed as argument to
     * this method.
     * <p>
     * The extension is registered if and only if the DeploymentUnit is part of a Weld Deployment. Specifically,
     * if a call to {@link #isPartOfWeldDeployment(DeploymentUnit)} using the DeploymentUnit argument
     * returns {@code true}. Otherwise, this method will return immediately.
     *
     * @param extension An instance of the CDI portable extension to add.
     * @param unit      The deployment unit where the extension will be registered.
     */
    void registerBuildCompatibleExtension(final Class<? extends BuildCompatibleExtension> extension, final DeploymentUnit unit);

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
     * Adds a bean defining annotation for every deployment on the server, treating it as inherited if it is annotated
     * with {@link Inherited}.
     *
     * @param annotation the annotation type, must not be {@code null}
     *
     * @return this capability
     *
     * @see #addBeanDefiningAnnotation(Class, boolean)
     */
    default WeldCapability addBeanDefiningAnnotation(final Class<? extends Annotation> annotation) {
        return addBeanDefiningAnnotation(annotation, annotation.isAnnotationPresent(Inherited.class));
    }

    /**
     * Adds a bean defining annotation for every deployment on the server.
     * <p>
     * The annotation is global, not scoped to the calling subsystem or to a deployment. A deployment reads the set of
     * annotations when it is deployed, so a deployment that is already deployed does not see an annotation added later
     * until it is redeployed. The annotations are cleared when the server reloads. Call this method from a subsystem add
     * operation so that the annotation is in place before any deployment is processed.
     * </p>
     *
     * @param annotation the annotation type, must not be {@code null}
     * @param inherited  {@code true} if the annotation is treated as {@linkplain Inherited inherited}
     *
     * @return this capability
     */
    WeldCapability addBeanDefiningAnnotation(Class<? extends Annotation> annotation, boolean inherited);

    /**
     * Returns a copy of the bean defining annotations added with {@link #addBeanDefiningAnnotation(Class, boolean)}.
     * <p>
     * The copy does not include the bean defining annotations that the CDI specification defines.
     * </p>
     *
     * @return an unmodifiable map from the annotation's fully qualified class name to whether it is treated as inherited
     */
    Map<String, Boolean> getBeanDefiningAnnotations();

}
