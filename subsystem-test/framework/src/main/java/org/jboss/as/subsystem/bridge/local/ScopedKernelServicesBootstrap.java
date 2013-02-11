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
package org.jboss.as.subsystem.bridge.local;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.model.test.ModelTestOperationValidatorFilter;
import org.jboss.as.subsystem.bridge.impl.ChildFirstClassLoaderKernelServicesFactory;
import org.jboss.as.subsystem.bridge.impl.ClassLoaderObjectConverterImpl;
import org.jboss.as.subsystem.bridge.impl.LegacyControllerKernelServicesProxy;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ScopedKernelServicesBootstrap {
    ClassLoader legacyChildFirstClassLoader;
    ClassLoaderObjectConverter objectConverter;

    public ScopedKernelServicesBootstrap(ClassLoader legacyChildFirstClassLoader) {
        this.legacyChildFirstClassLoader = legacyChildFirstClassLoader;
        this.objectConverter = new ClassLoaderObjectConverterImpl(this.getClass().getClassLoader(), legacyChildFirstClassLoader);
    }


    public LegacyControllerKernelServicesProxy createKernelServices(String mainSubsystemName, String extensionClassName, AdditionalInitialization additionalInit,
            ModelTestOperationValidatorFilter validateOpsFilter, List<ModelNode> bootOperations, ModelVersion legacyModelVersion, boolean persistXml) throws Exception {

        Object childClassLoaderKernelServices = createChildClassLoaderKernelServices(mainSubsystemName, extensionClassName, additionalInit, validateOpsFilter, bootOperations, legacyModelVersion, persistXml);
        return new LegacyControllerKernelServicesProxy(legacyChildFirstClassLoader, childClassLoaderKernelServices, objectConverter);
    }

    private Object createChildClassLoaderKernelServices(String mainSubsystemName, String extensionClassName, AdditionalInitialization additionalInit, ModelTestOperationValidatorFilter validateOpsFilter,
            List<ModelNode> bootOperations, ModelVersion legacyModelVersion, boolean persistXml){
        try {
            Class<?> clazz = legacyChildFirstClassLoader.loadClass(ChildFirstClassLoaderKernelServicesFactory.class.getName());

            Method m = clazz.getMethod("create",
                    String.class,
                    String.class,
                    legacyChildFirstClassLoader.loadClass(AdditionalInitialization.class.getName()),
                    legacyChildFirstClassLoader.loadClass(ModelTestOperationValidatorFilter.class.getName()),
                    List.class,
                    legacyChildFirstClassLoader.loadClass(ModelVersion.class.getName()),
                    Boolean.TYPE);

            List<Object> convertedBootOps = new ArrayList<Object>();
            for (int i = 0 ; i < bootOperations.size() ; i++) {
                ModelNode node = bootOperations.get(i);
                if (node != null) {
                    convertedBootOps.add(objectConverter.convertModelNodeToChildCl(node));
                }
            }

            //Convert additional Init
            Object convertedAdditionalInit = null;//TODO objectConverter.convertAdditionalInitializationToChildCl(additionalInit);
            Object convertedModelVersion = objectConverter.convertModelVersionToChildCl(legacyModelVersion);
            Object convertedValidateOpsFilter = objectConverter.convertValidateOperationsFilterToChildCl(validateOpsFilter);


            return m.invoke(null, mainSubsystemName, extensionClassName, convertedAdditionalInit, convertedValidateOpsFilter, convertedBootOps, convertedModelVersion, persistXml);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
