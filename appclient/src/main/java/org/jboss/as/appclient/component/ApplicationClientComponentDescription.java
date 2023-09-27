/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.appclient.component;


import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentFactory;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.invocation.InterceptorContext;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceName;

/**
 * @author Stuart Douglas
 */
public final class ApplicationClientComponentDescription extends ComponentDescription {


    public static final String APP_CLIENT_COMPONENT_NAME = "AppClientComponent";

    public ApplicationClientComponentDescription(final String componentClassName, final EEModuleDescription moduleDescription, final ServiceName deploymentUnitServiceName) {
        super(APP_CLIENT_COMPONENT_NAME, componentClassName, moduleDescription, deploymentUnitServiceName);
        setExcludeDefaultInterceptors(true);

    }


    public boolean isIntercepted() {
        return false;
    }

    @Override
    public ComponentConfiguration createConfiguration(final ClassReflectionIndex classIndex, final ClassLoader moduleClassLoader, final ModuleLoader moduleLoader) {
        final ComponentConfiguration configuration =  super.createConfiguration(classIndex, moduleClassLoader, moduleLoader);
        configuration.setInstanceFactory(new ComponentFactory() {
            @Override
            public ManagedReference create(final InterceptorContext context) {
                return new ManagedReference() {
                    @Override
                    public void release() {

                    }

                    @Override
                    public Object getInstance() {
                        return null;
                    }
                };
            }
        });
        return configuration;
    }
}
