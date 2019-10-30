/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.structure;

import org.jboss.as.ee.metadata.EJBClientDescriptorMetaData;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.metadata.ear.spec.EarMetaData;
import org.jboss.metadata.ear.spec.ModuleMetaData;
import org.jboss.vfs.VirtualFile;

/**
 * EE related attachments.
 *
 * @author John Bailey
 * @author Stuart Douglas
 */
public final class Attachments {

    public static final AttachmentKey<EarMetaData> EAR_METADATA = AttachmentKey.create(EarMetaData.class);

    /**
     * The distinct-name that is configured for the EE deployment, in the deployment descriptor
     */
    public static final AttachmentKey<String> DISTINCT_NAME = AttachmentKey.create(String.class);

    public static final AttachmentKey<ModuleMetaData> MODULE_META_DATA = AttachmentKey.create(ModuleMetaData.class);

    public static final AttachmentKey<EJBClientDescriptorMetaData> EJB_CLIENT_METADATA = AttachmentKey.create(EJBClientDescriptorMetaData.class);

    /**
     * The alternate deployment descriptor location
     */
    public static final AttachmentKey<VirtualFile> ALTERNATE_CLIENT_DEPLOYMENT_DESCRIPTOR = AttachmentKey.create(VirtualFile.class);
    public static final AttachmentKey<VirtualFile> ALTERNATE_WEB_DEPLOYMENT_DESCRIPTOR = AttachmentKey.create(VirtualFile.class);
    public static final AttachmentKey<VirtualFile> ALTERNATE_EJB_DEPLOYMENT_DESCRIPTOR = AttachmentKey.create(VirtualFile.class);
    public static final AttachmentKey<VirtualFile> ALTERNATE_CONNECTOR_DEPLOYMENT_DESCRIPTOR = AttachmentKey.create(VirtualFile.class);

    /**
     * A Marker that identifies the type of deployment
     */
    public static final AttachmentKey<DeploymentType> DEPLOYMENT_TYPE = AttachmentKey.create(DeploymentType.class);

    /**
     * If this is set to true property replacement will be enabled for spec descriptors
     */
    public static final AttachmentKey<Boolean> SPEC_DESCRIPTOR_PROPERTY_REPLACEMENT = AttachmentKey.create(Boolean.class);

    /**
     * If this is set to true property replacement will be enabled for jboss descriptors
     */
    public static final AttachmentKey<Boolean> JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT = AttachmentKey.create(Boolean.class);

    /**
     * If this is set to true property replacement will be enabled for EJB annotations
     */
    public static final AttachmentKey<Boolean> ANNOTATION_PROPERTY_REPLACEMENT = AttachmentKey.create(Boolean.class);


    private Attachments() {
    }
}
