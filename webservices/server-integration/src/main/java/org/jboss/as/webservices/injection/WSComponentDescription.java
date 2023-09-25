/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.webservices.injection;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class WSComponentDescription extends ComponentDescription {

    public WSComponentDescription(final String componentName, final String componentClassName,
            final EEModuleDescription moduleDescription, final ServiceName deploymentUnitServiceName) {
        super(componentName, componentClassName, moduleDescription, deploymentUnitServiceName);
        setExcludeDefaultInterceptors(true);
    }

    @Override
    public ComponentConfiguration createConfiguration(final ClassReflectionIndex classIndex, final ClassLoader moduleClassLoader,
            final ModuleLoader moduleLoader) {
        final ComponentConfiguration cc = super.createConfiguration(classIndex, moduleClassLoader, moduleLoader);
        cc.setComponentCreateServiceFactory(WSComponentCreateServiceFactory.INSTANCE);
        return cc;
    }

    @Override
    public boolean isCDIInterceptorEnabled() {
        return true;
    }
}
