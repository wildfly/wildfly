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
import org.jboss.osgi.framework.BundleManager;
import org.jboss.osgi.metadata.OSGiMetaData;
import org.jboss.osgi.spi.BundleInfo;
import org.osgi.framework.Bundle;

/**
 * OSGi Subsystem constants
 *
 * @author Thomas.Diesler@jboss.com
 */
public interface OSGiConstants {

    /** Service base name for all OSGi subsystem services. */
    ServiceName SERVICE_BASE_NAME = ServiceName.JBOSS.append("osgi", "as");

    /** Attachment key for the {@link BundleManager}. */
    AttachmentKey<BundleInfo> BUNDLE_INFO_KEY = AttachmentKey.create(BundleInfo.class);

    /** Attachment key for the {@link BundleManager}. */
    AttachmentKey<BundleManager> BUNDLE_MANAGER_KEY = AttachmentKey.create(BundleManager.class);

    /** Attachment key for a bundle deployment. */
    AttachmentKey<Deployment> DEPLOYMENT_KEY = AttachmentKey.create(Deployment.class);

    /** Attachment key for the installed bundle. */
    AttachmentKey<Bundle> INSTALLED_BUNDLE_KEY = AttachmentKey.create(Bundle.class);

    /** Attachment key for {@link OSGiMetaData}. */
    AttachmentKey<OSGiMetaData> OSGI_METADATA_KEY = AttachmentKey.create(OSGiMetaData.class);
}
