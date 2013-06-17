/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.osgi;

import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.msc.service.ServiceName;
import org.jboss.osgi.deployment.deployer.Deployment;
import org.jboss.osgi.framework.spi.BundleManager;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.resolver.XBundleRevision;
import org.jboss.osgi.resolver.XEnvironment;
import org.jboss.osgi.resolver.XResolver;
import org.jboss.osgi.spi.BundleInfo;
import org.osgi.framework.BundleContext;

/**
 * OSGi Subsystem constants
 *
 * @author Thomas.Diesler@jboss.com
 */
public interface OSGiConstants {

    /** Service base name for all OSGi subsystem services. */
    ServiceName SERVICE_BASE_NAME = ServiceName.JBOSS.append("osgi", "as");

    /** Attachment key for the {@link BundleInfo} when an OSGi bundle deployment is detected. */
    AttachmentKey<BundleInfo> BUNDLE_INFO_KEY = AttachmentKey.create(BundleInfo.class);

    /** Attachment key for the installed {@link XBundleRevision}. */
    AttachmentKey<XBundleRevision> BUNDLE_REVISION_KEY = AttachmentKey.create(XBundleRevision.class);

    /** Attachment key for the {@link BundleManager}. */
    AttachmentKey<BundleManager> BUNDLE_MANAGER_KEY = AttachmentKey.create(BundleManager.class);

    /** Attachment key set when deferred activation failed */
    AttachmentKey<Boolean> DEFERRED_ACTIVATION_FAILED = AttachmentKey.create(Boolean.class);

    /** Attachment key for a bundle deployment. */
    AttachmentKey<Deployment> DEPLOYMENT_KEY = AttachmentKey.create(Deployment.class);

    /** Attachment key for a bundle deployment. */
    AttachmentKey<DeploymentType> DEPLOYMENT_TYPE_KEY = AttachmentKey.create(DeploymentType.class);

    /** Attachment key for the {@link XEnvironment}. */
    AttachmentKey<XEnvironment> ENVIRONMENT_KEY = AttachmentKey.create(XEnvironment.class);

    /** Attachment key for {@link OSGiMetaData} */
    AttachmentKey<OSGiMetaData> OSGI_METADATA_KEY = AttachmentKey.create(OSGiMetaData.class);

    /** Attachment key for the {@link XResolver}. */
    AttachmentKey<XResolver> RESOLVER_KEY = AttachmentKey.create(XResolver.class);

    /** Attachment key for the OSGi system context. */
    AttachmentKey<BundleContext> SYSTEM_CONTEXT_KEY = AttachmentKey.create(BundleContext.class);

    /** The {@link org.jboss.as.osgi.parser.SubsystemState} service */
    ServiceName SUBSYSTEM_STATE_SERVICE_NAME = SERVICE_BASE_NAME.append("SubsystemState");

    /** The {@link org.jboss.osgi.provision.XResourceProvisioner} service */
    ServiceName PROVISIONER_SERVICE_NAME = SERVICE_BASE_NAME.append("XResourceProvisioner");

    /** The {@link org.jboss.osgi.resolver.XEnvironment} service */
    ServiceName ENVIRONMENT_SERVICE_NAME = SERVICE_BASE_NAME.append("XEnvironment");

    /** The {@link org.jboss.osgi.resolver.XEnvironment} service */
    ServiceName ABSTRACT_RESOLVER_SERVICE_NAME = SERVICE_BASE_NAME.append("XResolver");

    /** The OSGi deployment type */
    enum DeploymentType {
        /** A bundle deployment */
        Bundle,
        /** An adapted module deployment */
        Module
    }
}
