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
package org.jboss.as.webservices.webserviceref;

import static org.jboss.as.ee.utils.InjectionUtils.getInjectionTarget;
import static org.jboss.as.webservices.WSMessages.MESSAGES;
import static org.jboss.as.webservices.util.ASHelper.getAnnotations;
import static org.jboss.as.webservices.util.ASHelper.getWSRefRegistry;
import static org.jboss.as.webservices.util.DotNames.WEB_SERVICE_REFS_ANNOTATION;
import static org.jboss.as.webservices.util.DotNames.WEB_SERVICE_REF_ANNOTATION;
import static org.jboss.as.webservices.webserviceref.WSRefUtils.processAnnotatedElement;
import static org.jboss.as.webservices.webserviceref.WSRefUtils.processType;

import java.lang.reflect.AccessibleObject;
import java.util.List;

import javax.xml.ws.Service;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.FieldInjectionTarget;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.component.InjectionTarget;
import org.jboss.as.ee.component.LookupInjectionSource;
import org.jboss.as.ee.component.MethodInjectionTarget;
import org.jboss.as.ee.component.ResourceInjectionConfiguration;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.as.webservices.util.VirtualFileAdaptor;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.modules.Module;
import org.jboss.wsf.spi.deployment.UnifiedVirtualFile;
import org.jboss.wsf.spi.metadata.j2ee.serviceref.UnifiedServiceRefMetaData;

/**
 * @WebServiceRef annotation processor.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class WSRefAnnotationProcessor implements DeploymentUnitProcessor {

    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit unit = phaseContext.getDeploymentUnit();

        // Process @WebServiceRef annotations
        final List<AnnotationInstance> webServiceRefAnnotations = getAnnotations(unit, WEB_SERVICE_REF_ANNOTATION);
        for (final AnnotationInstance annotation : webServiceRefAnnotations) {
            final AnnotationTarget annotationTarget = annotation.target();
            final WSRefAnnotationWrapper annotationWrapper = new WSRefAnnotationWrapper(annotation);

            if (annotationTarget instanceof FieldInfo) {
                processFieldRef(unit, annotationWrapper, (FieldInfo) annotationTarget);
            } else if (annotationTarget instanceof MethodInfo) {
                processMethodRef(unit, annotationWrapper, (MethodInfo) annotationTarget);
            } else if (annotationTarget instanceof ClassInfo) {
                processClassRef(unit, annotationWrapper, (ClassInfo) annotationTarget);
            }
        }

        // Process @WebServiceRefs annotations
        final List<AnnotationInstance> webServiceRefsAnnotations = getAnnotations(unit, WEB_SERVICE_REFS_ANNOTATION);
        for (final AnnotationInstance outerAnnotation : webServiceRefsAnnotations) {
            final AnnotationTarget annotationTarget = outerAnnotation.target();
            if (annotationTarget instanceof ClassInfo) {
                final AnnotationInstance[] values = outerAnnotation.value("value").asNestedArray();
                for (final AnnotationInstance annotation : values) {
                    final WSRefAnnotationWrapper annotationWrapper = new WSRefAnnotationWrapper(annotation);
                    processClassRef(unit, annotationWrapper, (ClassInfo) annotationTarget);
                }
            }
        }
    }

    public void undeploy(final DeploymentUnit unit) {
        // NOOP
    }

    private static void processFieldRef(final DeploymentUnit unit, final WSRefAnnotationWrapper annotation, final FieldInfo fieldInfo) throws DeploymentUnitProcessingException {
        final String fieldName = fieldInfo.name();
        final String injectionType = isEmpty(annotation.type()) || annotation.type().equals(Object.class.getName()) ? fieldInfo.type().name().toString() : annotation.type();
        final InjectionTarget injectionTarget = new FieldInjectionTarget(fieldInfo.declaringClass().name().toString(),  fieldName, injectionType);
        final String bindingName = isEmpty(annotation.name()) ? fieldInfo.declaringClass().name().toString() + "/" + fieldInfo.name() : annotation.name();
        processRef(unit, injectionType, annotation, fieldInfo.declaringClass(), injectionTarget, bindingName);
    }

    private static void processMethodRef(final DeploymentUnit unit, final WSRefAnnotationWrapper annotation, final MethodInfo methodInfo) throws DeploymentUnitProcessingException {
        final String methodName = methodInfo.name();
        if (!methodName.startsWith("set") || methodInfo.args().length != 1) {
            throw MESSAGES.invalidServiceRefSetterMethodName(methodInfo);
        }
        final String injectionType = isEmpty(annotation.type()) || annotation.type().equals(Object.class.getName()) ? methodInfo.args()[0].name().toString() : annotation.type();
        final InjectionTarget injectionTarget = new MethodInjectionTarget(methodInfo.declaringClass().name().toString(), methodName, injectionType);
        final String bindingName = isEmpty(annotation.name()) ? methodInfo.declaringClass().name().toString() + "/" + methodName.substring(3, 4).toLowerCase() + methodName.substring(4) : annotation.name();
        processRef(unit, injectionType, annotation, methodInfo.declaringClass(), injectionTarget, bindingName);
    }

    private static void processClassRef(final DeploymentUnit unit, final WSRefAnnotationWrapper annotation, final ClassInfo classInfo) throws DeploymentUnitProcessingException {
        if (isEmpty(annotation.name())) {
            throw MESSAGES.requiredServiceRefName();
        }
        if (isEmpty(annotation.type())) {
            throw MESSAGES.requiredServiceRefType();
        }
        processRef(unit, annotation.type(), annotation, classInfo, null, annotation.name());
    }

    private static void processRef(final DeploymentUnit unit, final String type, final WSRefAnnotationWrapper annotation, final ClassInfo classInfo, final InjectionTarget injectionTarget, final String bindingName) throws DeploymentUnitProcessingException {
        boolean isEJB = false;
        final EEModuleDescription moduleDescription = unit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final String componentClassName = classInfo.name().toString();
        final Module module = unit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        for (final ComponentDescription componentDescription : moduleDescription.getComponentsByClassName(componentClassName)) {
            if (componentDescription instanceof SessionBeanComponentDescription) {
                isEJB = true;

                final UnifiedServiceRefMetaData serviceRefUMDM = getServiceRef(unit, componentDescription, bindingName);
                initServiceRef(unit, serviceRefUMDM, type, annotation);
                processWSFeatures(unit, serviceRefUMDM, injectionTarget, classInfo);

                // Create the binding from whence our injection comes.
                final InjectionSource serviceRefSource = new WSRefValueSource(serviceRefUMDM, module.getClassLoader());
                final BindingConfiguration bindingConfiguration = new BindingConfiguration(bindingName, serviceRefSource);
                componentDescription.getBindingConfigurations().add(bindingConfiguration);
                // our injection comes from the local lookup, no matter what.
                final ResourceInjectionConfiguration injectionConfiguration = injectionTarget != null ? new ResourceInjectionConfiguration(injectionTarget, new LookupInjectionSource(bindingName)) : null;
                if (injectionConfiguration != null) {
                    componentDescription.addResourceInjection(injectionConfiguration);
                }
            }
        }
        if (!isEJB) {
            final UnifiedServiceRefMetaData serviceRefUMDM = getServiceRef(unit, null, bindingName);
            initServiceRef(unit, serviceRefUMDM, type, annotation);
            processWSFeatures(unit, serviceRefUMDM, injectionTarget, classInfo);

            // TODO: class hierarchies? shared bindings?
            final EEModuleClassDescription classDescription = moduleDescription.addOrGetLocalClassDescription(classInfo.name().toString());
            // Create the binding from whence our injection comes.
            final InjectionSource serviceRefSource = new WSRefValueSource(serviceRefUMDM, module.getClassLoader());
            final BindingConfiguration bindingConfiguration = new BindingConfiguration(bindingName, serviceRefSource);
            classDescription.getBindingConfigurations().add(bindingConfiguration);
            // our injection comes from the local lookup, no matter what.
            final ResourceInjectionConfiguration injectionConfiguration = injectionTarget != null ?
                new ResourceInjectionConfiguration(injectionTarget, new LookupInjectionSource(bindingName)) : null;
            if (injectionConfiguration != null) {
                classDescription.addResourceInjection(injectionConfiguration);
            }
        }
    }

    private static void processWSFeatures(final DeploymentUnit unit, final UnifiedServiceRefMetaData serviceRefUMDM, final InjectionTarget injectionTarget, final ClassInfo classInfo) throws DeploymentUnitProcessingException {
        if (injectionTarget != null) {
            // @WebServiceRef specified on field or method
            processInjectionTarget(unit, serviceRefUMDM, injectionTarget);
        } else {
            // @WebServiceRef specified on class
            processInjectionTarget(unit, serviceRefUMDM, classInfo);
        }
    }

    private static UnifiedServiceRefMetaData getServiceRef(final DeploymentUnit unit, final ComponentDescription componentDescription, final String serviceRefName) {
        final WSReferences wsRefRegistry = getWSRefRegistry(unit);
        final String cacheKey = getCacheKey(componentDescription, serviceRefName);
        UnifiedServiceRefMetaData serviceRefUMDM = wsRefRegistry.get(cacheKey);
        if (serviceRefUMDM == null) {
            serviceRefUMDM = new UnifiedServiceRefMetaData(getUnifiedVirtualFile(unit));
            serviceRefUMDM.setServiceRefName(serviceRefName);
            wsRefRegistry.add(cacheKey, serviceRefUMDM);
        }
        return serviceRefUMDM;
    }

    private static String getCacheKey(final ComponentDescription componentDescription, final String serviceRefName) {
        if (componentDescription == null) {
            return serviceRefName;
        } else {
            return componentDescription.getComponentName() + "/" + serviceRefName;
        }
    }

    private static void processInjectionTarget(final DeploymentUnit unit, final UnifiedServiceRefMetaData serviceRefUMDM, final ClassInfo classInfo) throws DeploymentUnitProcessingException {
        final Module module = unit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final Class<?> target = getClass(module.getClassLoader(), classInfo.name().toString());
        processAnnotatedElement(target, serviceRefUMDM);
    }

    private static void processInjectionTarget(final DeploymentUnit unit, final UnifiedServiceRefMetaData serviceRefUMDM, final InjectionTarget injectionTarget) throws DeploymentUnitProcessingException {
        final Module module = unit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final DeploymentReflectionIndex deploymentReflectionIndex = unit.getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);
        final String injectionTargetClassName = injectionTarget.getClassName();
        final String injectionTargetName = getInjectionTargetName(injectionTarget);
        final AccessibleObject fieldOrMethod = getInjectionTarget(injectionTargetClassName, injectionTargetName, module.getClassLoader(), deploymentReflectionIndex);
        processAnnotatedElement(fieldOrMethod, serviceRefUMDM);
    }

    private static String getInjectionTargetName(final InjectionTarget injectionTarget) {
        final String name = injectionTarget.getName();
        if (injectionTarget instanceof FieldInjectionTarget) {
            return name;
        } else if (injectionTarget instanceof MethodInjectionTarget) {
            return name.substring(3, 4).toUpperCase() + name.substring(4);
        }
        throw new UnsupportedOperationException();
    }

    private static UnifiedServiceRefMetaData initServiceRef(final DeploymentUnit unit, final UnifiedServiceRefMetaData serviceRefUMDM, final String type, final WSRefAnnotationWrapper annotation) throws DeploymentUnitProcessingException  {
        // wsdl location
        if (!isEmpty(annotation.wsdlLocation())) {
            serviceRefUMDM.setWsdlFile(annotation.wsdlLocation());
        }
        // ref class type
        final Module module = unit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final Class<?> typeClass = getClass(module.getClassLoader(), type);
        serviceRefUMDM.setServiceRefType(typeClass.getName());
        // ref service interface
        if (!isEmpty(annotation.value())) {
            serviceRefUMDM.setServiceInterface(annotation.value());
        } else if (Service.class.isAssignableFrom(typeClass)) {
            serviceRefUMDM.setServiceInterface(typeClass.getName());
        } else {
            serviceRefUMDM.setServiceInterface(Service.class.getName());
        }
        // ref type
        processType(serviceRefUMDM);

        return serviceRefUMDM;
    }

    private static Class<?> getClass(final ClassLoader classLoader, final String className) throws DeploymentUnitProcessingException { // TODO: refactor to common code
        if (!isEmpty(className)) {
            try {
                return classLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new DeploymentUnitProcessingException(e);
            }
        }

        return null;
    }

    private static UnifiedVirtualFile getUnifiedVirtualFile(final DeploymentUnit unit) { // TODO: refactor to common code
        final ResourceRoot resourceRoot = unit.getAttachment(org.jboss.as.server.deployment.Attachments.DEPLOYMENT_ROOT);
        return new VirtualFileAdaptor(resourceRoot.getRoot());
    }

    private static boolean isEmpty(final String string) { // TODO: some common class - StringUtils ?
        return string == null || string.isEmpty();
    }

}
