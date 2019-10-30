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

package org.jboss.as.web.common;


import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.msc.service.ServiceName;

/**
 * @author Stuart Douglas
 */
public final class WebComponentDescription extends ComponentDescription {

    public static final AttachmentKey<AttachmentList<ServiceName>> WEB_COMPONENTS = AttachmentKey.createList(ServiceName.class);

    public WebComponentDescription(final String componentName, final String componentClassName, final EEModuleDescription moduleDescription, final ServiceName deploymentUnitServiceName, final EEApplicationClasses applicationClassesDescription) {
        super(componentName, componentClassName, moduleDescription, deploymentUnitServiceName);
        setExcludeDefaultInterceptors(true);
    }


    public boolean isIntercepted() {
        return false;
    }

    /**
     * Web components are optional. If they are actually required we leave it up to the web subsystem to error out.
     * @return <code>true</code>
     */
    @Override
    public boolean isOptional() {
        return true;
    }
}
