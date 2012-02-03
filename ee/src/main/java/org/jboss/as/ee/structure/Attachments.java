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
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.metadata.ear.jboss.JBossAppMetaData;
import org.jboss.metadata.ear.spec.EarMetaData;
import org.jboss.metadata.ear.spec.ModuleMetaData;
import org.jboss.msc.service.ServiceName;
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
     * A Marker that identifies the a type of deployment
     */
    public static final AttachmentKey<DeploymentType> DEPLOYMENT_TYPE = AttachmentKey.create(DeploymentType.class);


    /**
     * Services that must be waited on for this deployment if initialize in order is enabled. Only once all these services are up can the next deployment
     * start. It is not necessary to add component start services to this list, they are handed automatically.
     *
     * These entries must be added before the {@link org.jboss.as.server.deployment.Phase#POST_MODULE} phase, as this is the phase where the dependencies are
     * set up.
     */
    public static final AttachmentKey<AttachmentList<ServiceName>> INITIALISE_IN_ORDER_SERVICES = AttachmentKey.createList(ServiceName.class);

    private Attachments() {
    }
}
