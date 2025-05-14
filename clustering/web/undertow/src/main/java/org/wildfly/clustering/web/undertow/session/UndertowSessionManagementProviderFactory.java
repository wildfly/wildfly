/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.session;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.metadata.web.jboss.ReplicationConfig;
import org.jboss.modules.Module;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.immutable.Immutability;
import org.wildfly.clustering.web.container.SessionManagementProvider;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;
import org.wildfly.clustering.web.service.session.LegacyDistributableSessionManagementProviderFactory;
import org.wildfly.clustering.web.undertow.logging.UndertowClusteringLogger;
import org.wildfly.extension.undertow.session.SessionManagementProviderFactory;

/**
 * {@link SessionManagementProviderFactory} for Undertow using either an attached {@link DistributableSessionManagementProvider} or generated from a legacy {@link ReplicationConfig}.
 * @author Paul Ferraro
 */
@SuppressWarnings("deprecation")
@MetaInfServices(SessionManagementProviderFactory.class)
public class UndertowSessionManagementProviderFactory implements SessionManagementProviderFactory {

    private final LegacyDistributableSessionManagementProviderFactory legacyFactory = ServiceLoader.load(LegacyDistributableSessionManagementProviderFactory.class, LegacyDistributableSessionManagementProviderFactory.class.getClassLoader()).findFirst().orElseThrow();

    @Override
    public SessionManagementProvider createSessionManagementProvider(DeploymentPhaseContext context, ReplicationConfig config) {
        DeploymentUnit unit = context.getDeploymentUnit();
        DistributableSessionManagementProvider provider = unit.getAttachment(DistributableSessionManagementProvider.ATTACHMENT_KEY);
        // For compatibility, honor legacy <replication-config/> over an attached provider
        if ((config != null) || (provider == null)) {
            if (provider != null) {
                UndertowClusteringLogger.ROOT_LOGGER.legacySessionManagementProviderOverride(unit.getName());
            } else {
                UndertowClusteringLogger.ROOT_LOGGER.legacySessionManagementProviderInUse(unit.getName());
            }
            // Fabricate DistributableSessionManagementProvider from legacy ReplicationConfig
            provider = this.legacyFactory.createSessionManagerProvider(unit, config);
        }
        Module module = unit.getAttachment(Attachments.MODULE);
        List<String> immutableClassNames = unit.getAttachmentList(DistributableSessionManagementProvider.IMMUTABILITY_ATTACHMENT_KEY);
        List<Class<?>> immutableClasses = new ArrayList<>(immutableClassNames.size());
        try {
            for (String immutableClassName : immutableClassNames) {
                immutableClasses.add(module.getClassLoader().loadClass(immutableClassName));
            }
            return new UndertowDistributableSessionManagementProvider(provider, Immutability.classes(immutableClasses));
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
