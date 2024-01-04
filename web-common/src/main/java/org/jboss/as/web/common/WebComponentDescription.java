/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
