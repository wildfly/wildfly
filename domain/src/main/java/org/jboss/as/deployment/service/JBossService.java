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

package org.jboss.as.deployment.service;

import org.jboss.modules.Module;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;

import java.lang.reflect.Method;

/**
 * Service wrapper for legacy JBoss MBean services.
 *
 * @author John E. Bailey
 */
public class JBossService<T> implements Service<T> {
    private Value<T> serviceValue;

    private final InjectedValue<Module> deploymentModule = new InjectedValue<Module>();
    private final Value<ClassLoader> deploymentClassLoaderValue = new Value<ClassLoader>() {
            @Override
            public ClassLoader getValue() throws IllegalStateException {
                return deploymentModule.getValue().getClassLoader();
            }
        };

    @Override
    public void start(StartContext context) throws StartException {
        final T service = getValue();
        // Handle Start
        try {
            Method startMethod = service.getClass().getMethod("start");
            startMethod.invoke(service);
        } catch(NoSuchMethodException e) {
            // Log warning ???
        } catch(Exception e) {
            throw new StartException("Failed to execute legacy service start", e);
        }
    }

    @Override
    public void stop(StopContext context) {
        final T service = getValue();
        // Handle Stop
        try {
            Method startMethod = service.getClass().getMethod("stop");
            startMethod.invoke(service);
        } catch(Exception e) {
            // Log warning ???
        }
    }

    @Override
    public T getValue() throws IllegalStateException {
        return serviceValue.getValue();
    }

    public void setServiceValue(Value<T> serviceValue) {
        this.serviceValue = serviceValue;
    }

    public Injector<Module> getDeploymentModuleInjector() {
        return deploymentModule;
    }

    public Value<ClassLoader> getDeploymentClassLoaderValue() {
        return deploymentClassLoaderValue;
    }
}
