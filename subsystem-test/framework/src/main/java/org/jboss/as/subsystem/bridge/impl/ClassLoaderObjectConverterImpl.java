/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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

import java.io.IOException;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.model.test.ModelTestOperationValidatorFilter;
import org.jboss.as.subsystem.bridge.local.ClassLoaderObjectConverter;
import org.jboss.as.subsystem.bridge.shared.ObjectSerializer;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ClassLoaderObjectConverterImpl implements ClassLoaderObjectConverter{
    private final ObjectSerializer local;
    private final ObjectSerializer remote;

    public ClassLoaderObjectConverterImpl(final ClassLoader local, final ClassLoader remote) {
        this.local = ObjectSerializer.FACTORY.createSerializer(local);
        this.remote = ObjectSerializer.FACTORY.createSerializer(remote);

        if (this.local.getClass().getClassLoader() != local) {
            throw new IllegalStateException("Wrong classloader");
        }
        if (this.remote.getClass().getClassLoader() != remote) {
            throw new IllegalStateException("Wrong classloader");
        }
    }

    public Object convertModelNodeToChildCl(ModelNode modelNode) {
        if (modelNode == null) {
            return null;
        }
        try {
            return remote.deserializeModelNode(local.serializeModelNode(modelNode));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public ModelNode convertModelNodeFromChildCl(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return (ModelNode)local.deserializeModelNode(remote.serializeModelNode(object));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object convertModelVersionToChildCl(ModelVersion modelVersion) {
        if (modelVersion == null) {
            return null;
        }
        return remote.deserializeModelVersion(local.serializeModelVersion(modelVersion));
    }

    @Override
    public Object convertAdditionalInitializationToChildCl(AdditionalInitialization additionalInit) {
        try {
            return remote.deserializeAdditionalInitialization(local.serializeAdditionalInitialization(additionalInit));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Object convertValidateOperationsFilterToChildCl(ModelTestOperationValidatorFilter validateOpsFilter) {
        try {
            return remote.deserializeModelTestOperationValidatorFilter(local.serializeModelTestOperationValidatorFilter(validateOpsFilter));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
