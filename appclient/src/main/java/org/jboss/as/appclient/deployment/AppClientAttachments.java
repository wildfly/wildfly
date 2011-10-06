/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.appclient.deployment;

import org.jboss.as.appclient.component.ApplicationClientComponentDescription;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.metadata.appclient.spec.ApplicationClientMetaData;

/**
 * @author Stuart Douglas
 */
public class AppClientAttachments {

    public static final AttachmentKey<Class<?>> MAIN_CLASS = AttachmentKey.create(Class.class);

    public static final AttachmentKey<Boolean> START_APP_CLIENT = AttachmentKey.create(Boolean.class);

    public static final AttachmentKey<ApplicationClientMetaData> APPLICATION_CLIENT_META_DATA = AttachmentKey.create(ApplicationClientMetaData.class);

    public static final AttachmentKey<ApplicationClientComponentDescription> APPLICATION_CLIENT_COMPONENT = AttachmentKey.create(ApplicationClientComponentDescription.class);

    private AppClientAttachments() {
    }
}
