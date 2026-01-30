/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheProvider;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.wildfly.subsystem.service.ServiceInstaller;

/**
 * Installs services required for stateful session bean caches.
 * @author Paul Ferraro
 */
public class CacheDependenciesProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext context) {
        DeploymentUnit unit = context.getDeploymentUnit();
        // Attachments may contain duplicates (if providers were referenced via an alias ServiceName)
        for (StatefulSessionBeanCacheProvider provider : context.getAttachmentList(StatefulSessionBeanCacheProvider.ATTACHMENT_KEY).stream().distinct().toList()) {
            for (ServiceInstaller installer : provider.getDeploymentServiceInstallers(unit)) {
                installer.install(context);
            }
        }
    }
}
