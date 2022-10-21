/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.web.undertow.session;

import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.metadata.web.jboss.ReplicationConfig;
import org.jboss.modules.Module;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.ee.immutable.SimpleImmutability;
import org.wildfly.clustering.web.container.SessionManagementProvider;
import org.wildfly.clustering.web.service.session.DistributableSessionManagementProvider;
import org.wildfly.clustering.web.service.session.LegacySessionManagementProviderFactory;
import org.wildfly.clustering.web.session.DistributableSessionManagementConfiguration;
import org.wildfly.clustering.web.undertow.logging.UndertowClusteringLogger;
import org.wildfly.extension.undertow.session.SessionManagementProviderFactory;

/**
 * {@link SessionManagementProviderFactory} for Undertow using either an attached {@link DistributableSessionManagementProvider} or generated from a legacy {@link ReplicationConfig}.
 * @author Paul Ferraro
 */
@SuppressWarnings("deprecation")
@MetaInfServices(SessionManagementProviderFactory.class)
public class UndertowSessionManagementProviderFactory implements SessionManagementProviderFactory {

    private final LegacySessionManagementProviderFactory<DistributableSessionManagementConfiguration<DeploymentUnit>> legacyFactory;

    public UndertowSessionManagementProviderFactory() {
        @SuppressWarnings("rawtypes")
        Iterator<LegacySessionManagementProviderFactory> factories = ServiceLoader.load(LegacySessionManagementProviderFactory.class, LegacySessionManagementProviderFactory.class.getClassLoader()).iterator();
        if (!factories.hasNext()) {
            throw new ServiceConfigurationError(LegacySessionManagementProviderFactory.class.getName());
        }
        this.legacyFactory = factories.next();
    }

    @Override
    public SessionManagementProvider createSessionManagementProvider(DeploymentUnit unit, ReplicationConfig config) {
        DistributableSessionManagementProvider<DistributableSessionManagementConfiguration<DeploymentUnit>> provider = unit.getAttachment(DistributableSessionManagementProvider.ATTACHMENT_KEY);
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
        List<String> immutableClasses = unit.getAttachmentList(DistributableSessionManagementProvider.IMMUTABILITY_ATTACHMENT_KEY);
        return new UndertowDistributableSessionManagementProvider<>(provider, new SimpleImmutability(module.getClassLoader(), immutableClasses));
    }
}
