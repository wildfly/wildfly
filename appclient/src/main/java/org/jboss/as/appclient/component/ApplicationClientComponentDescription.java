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
    };
}
