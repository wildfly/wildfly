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
package org.jboss.as.webservices.deployers.annotation;

import javax.xml.ws.Service;

import org.jboss.as.ee.component.InjectionTargetDescription;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.wsf.spi.deployment.UnifiedVirtualFile;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedServiceRefMetaData;
import org.jboss.wsf.spi.serviceref.ServiceRefHandler.Type;

/**
 * Base processor for @WebServiceRef annotations.
 *
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public abstract class AbstractWebServiceRefAnnotation<E extends AnnotationTarget> {

    protected final CompositeIndex index;

    protected AbstractWebServiceRefAnnotation(CompositeIndex index) {
        this.index = index;
    }

    public UnifiedServiceRefMetaData process(AnnotationInstance annotationInstance, E annotationTarget, UnifiedVirtualFile vfs) {
        if (annotationInstance == null)
            return null;
        return createServiceRef(annotationInstance, annotationTarget, vfs);
    }

    protected UnifiedServiceRefMetaData createServiceRef(AnnotationInstance annotationInstance, E annotationTarget, UnifiedVirtualFile vfs) {
        final UnifiedServiceRefMetaData ref = new UnifiedServiceRefMetaData(vfs);
        final AnnotationValue declaredNameValue = annotationInstance.value("name");
        String nameValue = declaredNameValue == null ? "" : declaredNameValue.asString();
        if (nameValue.length() == 0)
            nameValue = getName(annotationTarget);
        ref.setServiceRefName(nameValue);
        // TODO handle mappedName
        // final AnnotationValue declaredMappedNameValue = annotationInstance.value("mappedName");
        //String mappedNameValue = declaredMappedNameValue == null ? "" : declaredMappedNameValue.asString();
        final AnnotationValue declaredWsdlLocation = annotationInstance.value("wsdlLocation");
        String wsdlLocationValue = declaredWsdlLocation == null ? "" : declaredWsdlLocation.asString();
        if (wsdlLocationValue.length() > 0)
            ref.setWsdlFile(wsdlLocationValue);
        final AnnotationValue declaredTypeValue = annotationInstance.value("type");
        DotName declaredType = declaredTypeValue != null ? declaredTypeValue.asClass().name() : null;
        if (declaredType != null && declaredType.toString() != Object.class.getName()) {
            ref.setServiceRefType(declaredType.toString());
        } else
            ref.setServiceRefType(getType(annotationTarget));

        final AnnotationValue declaredValue = annotationInstance.value("value");
        DotName value = declaredValue != null ? declaredValue.asClass().name() : null;
        if (declaredValue != null && !declaredValue.toString().equals(Service.class.getName())) {
            ref.setServiceInterface(value.toString());
        }
        else {
            ClassInfo targetClass = getTypeInfo(annotationTarget);
            // FIXME
            if (targetClass != null/* && isAssignableFrom(targetClass, DotName.createSimple(Service.class.getName()))*/) {
                ref.setServiceInterface(getTypeInfo(annotationTarget).name().toString());
            }
        }

        final boolean isJAXRPC = ref.getMappingFile() != null // TODO: is mappingFile check required?
        || "javax.xml.rpc.Service".equals(ref.getServiceInterface());
        ref.setType(isJAXRPC ? Type.JAXRPC : Type.JAXWS);
        return ref;
    }

    private boolean isAssignableFrom(ClassInfo classInfo, DotName className) {
        if (classInfo.name().equals(className)) {
            return true;
        }
        ClassInfo superClass = index.getClassByName(classInfo.superName());
        if (superClass != null && isAssignableFrom(superClass, className)) {
            return true;
        }
        for (DotName superInterface :classInfo.interfaces()) {
            ClassInfo interfaceInfo = index.getClassByName(superInterface);
            if (interfaceInfo != null && isAssignableFrom(interfaceInfo, className)) {
                return true;
            }
        }
        return false;
    }

    public abstract String getName(E annotationTarget);
    public abstract InjectionTargetDescription.Type getInjectionType();
    protected abstract String getInjectionName(E element);
    protected abstract String getType(E element);
    protected abstract ClassInfo getTypeInfo(E element);
    protected abstract String getDeclaringClass(E element);
}
