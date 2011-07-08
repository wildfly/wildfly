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

package org.jboss.as.ee.component;

import org.jboss.as.server.deployment.AttachmentKey;

/**
 * @author John Bailey
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class Attachments {

    public static final AttachmentKey<EEApplicationDescription> EE_APPLICATION_DESCRIPTION = AttachmentKey.create(EEApplicationDescription.class);
    public static final AttachmentKey<EEApplicationClasses> EE_APPLICATION_CLASSES_DESCRIPTION = AttachmentKey.create(EEApplicationClasses.class);


    public static final AttachmentKey<EEModuleDescription> EE_MODULE_DESCRIPTION = AttachmentKey.create(EEModuleDescription.class);
    public static final AttachmentKey<EEModuleConfiguration> EE_MODULE_CONFIGURATION = AttachmentKey.create(EEModuleConfiguration.class);

    public static final AttachmentKey<DeploymentDescriptorEnvironment> MODULE_DEPLOYMENT_DESCRIPTOR_ENVIRONMENT = AttachmentKey.create(DeploymentDescriptorEnvironment.class);

}
