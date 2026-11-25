/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.component.stateful.cache;

import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.wildfly.service.descriptor.NullaryServiceDescriptor;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Provides configurators for services to install a stateful session bean cache factory.
 * @author Paul Ferraro
 */
public interface StatefulSessionBeanCacheProvider {
    NullaryServiceDescriptor<StatefulSessionBeanCacheProvider> PASSIVATION_DISABLED_SERVICE_DESCRIPTOR = NullaryServiceDescriptor.of("org.wildfly.ejb.stateful.passation-disabled-cache", StatefulSessionBeanCacheProvider.class);
    NullaryServiceDescriptor<StatefulSessionBeanCacheProvider> DEFAULT_SERVICE_DESCRIPTOR = NullaryServiceDescriptor.of("org.wildfly.ejb.stateful.default-cache", StatefulSessionBeanCacheProvider.class);
    UnaryServiceDescriptor<StatefulSessionBeanCacheProvider> SERVICE_DESCRIPTOR = UnaryServiceDescriptor.of("org.wildfly.ejb.stateful.cache", DEFAULT_SERVICE_DESCRIPTOR);

    AttachmentKey<AttachmentList<StatefulSessionBeanCacheProvider>> ATTACHMENT_KEY = AttachmentKey.createList(StatefulSessionBeanCacheProvider.class);

    /**
     * Returns zero or more service installers required to support SFSB caching for a deployment.
     * @param unit a deployment unit
     * @return zero or more service installers
     */
    Iterable<ServiceInstaller> getDeploymentServiceInstallers(DeploymentUnit unit);

    /**
     * Returns zero or more service installers required to support caching for the specified SFSB component.
     * @param unit the deployment unit containing the SFSB component.
     * @param description the description of the SFSB component
     * @return zero or more service installers
     */
    Iterable<ServiceInstaller> getStatefulBeanCacheFactoryServiceInstallers(DeploymentUnit unit, StatefulComponentDescription description);

    /**
     * Indicates whether or not cache factories provides by this object can support passivation.
     * @return true, if passivation is supported, false otherwise.
     */
    boolean supportsPassivation();
}
