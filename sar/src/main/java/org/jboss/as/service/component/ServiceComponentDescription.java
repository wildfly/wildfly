/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.service.component;

import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.ComponentFactory;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.naming.ManagedReference;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.invocation.InterceptorContext;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceName;

/**
 * @author Eduardo Martins
 */
public final class ServiceComponentDescription extends ComponentDescription {

    public ServiceComponentDescription(final String componentName, final String componentClassName,
            final EEModuleDescription moduleDescription, final ServiceName deploymentUnitServiceName,
            final EEApplicationClasses applicationClassesDescription) {
        super(componentName, componentClassName, moduleDescription, deploymentUnitServiceName);
        setExcludeDefaultInterceptors(true);
    }

    public boolean isIntercepted() {
        return false;
    }

    @Override
    public ComponentConfiguration createConfiguration(final ClassReflectionIndex classIndex, final ClassLoader moduleClassLoader,
            final ModuleLoader moduleLoader) {
        final ComponentConfiguration configuration = super.createConfiguration(classIndex, moduleClassLoader, moduleLoader);
        // will not be used, but if instance factory is not set then components must have default constructor, which is not a
        // requirement for MBeans
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
