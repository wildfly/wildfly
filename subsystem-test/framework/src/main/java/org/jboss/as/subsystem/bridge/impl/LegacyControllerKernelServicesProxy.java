/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.subsystem.bridge.impl;

import java.lang.reflect.Method;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.model.test.ModelTestLegacyControllerKernelServicesProxy;
import org.jboss.as.subsystem.bridge.local.ClassLoaderObjectConverter;
import org.jboss.as.subsystem.bridge.local.OperationTransactionControlProxy;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.as.subsystem.test.KernelServicesInternal;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class LegacyControllerKernelServicesProxy extends ModelTestLegacyControllerKernelServicesProxy implements KernelServices, KernelServicesInternal {

    private final ClassLoaderObjectConverter converter;
    private Method readFullModelDescription;

    public LegacyControllerKernelServicesProxy(ClassLoader childFirstClassLoader, Object childFirstClassLoaderServices, ClassLoaderObjectConverter converter) {
        super(childFirstClassLoader, childFirstClassLoaderServices);
        this.converter = converter;
    }

    @Override
    public KernelServices getLegacyServices(ModelVersion modelVersion) {
        throw new IllegalStateException("Can only be called for the main controller");
    }

    @Override
    protected Object convertModelNodeToChildCl(ModelNode modelNode) {
        return converter.convertModelNodeToChildCl(modelNode);
    }


    @Override
    protected ModelNode convertModelNodeFromChildCl(Object object) {
        return converter.convertModelNodeFromChildCl(object);
    }


    @Override
    protected String getOperationTransactionProxyClassName() {
        return OperationTransactionControlProxy.class.getName();
    }

    @Override
    public ModelNode readFullModelDescription(ModelNode pathAddress) {
        try {
            if (readFullModelDescription == null) {
                readFullModelDescription = childFirstClassLoaderServices.getClass().getMethod("readFullModelDescription",
                        childFirstClassLoader.loadClass(ModelNode.class.getName()));
            }
            return convertModelNodeFromChildCl(
                    readFullModelDescription.invoke(childFirstClassLoaderServices,
                            convertModelNodeToChildCl(pathAddress)));
        } catch (Exception e) {
            unwrapInvocationTargetRuntimeException(e);
            throw new RuntimeException(e);
        }
    }
}
