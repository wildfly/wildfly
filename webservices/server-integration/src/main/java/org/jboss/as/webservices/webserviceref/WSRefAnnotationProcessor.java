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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.FieldInjectionTarget;
import org.jboss.as.ee.component.FixedInjectionSource;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.component.InjectionTarget;
import org.jboss.as.ee.component.LookupInjectionSource;
import org.jboss.as.ee.component.MethodInjectionTarget;
import org.jboss.as.ee.component.ResourceInjectionConfiguration;
import org.jboss.as.ee.utils.ClassLoadingUtils;
import org.jboss.as.ejb3.component.session.SessionBeanComponentDescription;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.as.webservices.logging.WSLogger;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.modules.Module;

import static org.jboss.as.ee.utils.InjectionUtils.getInjectionTarget;
import static org.jboss.as.webservices.util.ASHelper.getAnnotations;
import static org.jboss.as.webservices.util.DotNames.WEB_SERVICE_REFS_ANNOTATION;
import static org.jboss.as.webservices.util.DotNames.WEB_SERVICE_REF_ANNOTATION;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @WebServiceRef annotation processor.
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
        final InjectionTarget injectionTarget = new FieldInjectionTarget(fieldInfo.declaringClass().name().toString(), fieldName, injectionType);
        final String bindingName = isEmpty(annotation.name()) ? fieldInfo.declaringClass().name().toString() + "/" + fieldInfo.name() : annotation.name();
        processRef(unit, injectionType, annotation, fieldInfo.declaringClass(), injectionTarget, bindingName);
    }

    private static void processMethodRef(final DeploymentUnit unit, final WSRefAnnotationWrapper annotation, final MethodInfo methodInfo) throws DeploymentUnitProcessingException {
        final String methodName = methodInfo.name();
        if (!methodName.startsWith("set") || methodInfo.args().length != 1) {
            throw WSLogger.ROOT_LOGGER.invalidServiceRefSetterMethodName(methodInfo);
        }
        final String injectionType = isEmpty(annotation.type()) || annotation.type().equals(Object.class.getName()) ? methodInfo.args()[0].name().toString() : annotation.type();
        final InjectionTarget injectionTarget = new MethodInjectionTarget(methodInfo.declaringClass().name().toString(), methodName, injectionType);
        final String bindingName = isEmpty(annotation.name()) ? methodInfo.declaringClass().name().toString() + "/" + methodName.substring(3, 4).toLowerCase(Locale.ENGLISH) + methodName.substring(4) : annotation.name();
        processRef(unit, injectionType, annotation, methodInfo.declaringClass(), injectionTarget, bindingName);
    }

    private static void processClassRef(final DeploymentUnit unit, final WSRefAnnotationWrapper annotation, final ClassInfo classInfo) throws DeploymentUnitProcessingException {
        if (isEmpty(annotation.name())) {
            throw WSLogger.ROOT_LOGGER.requiredServiceRefName();
        }
        if (isEmpty(annotation.type())) {
            throw WSLogger.ROOT_LOGGER.requiredServiceRefType();
        }
        processRef(unit, annotation.type(), annotation, classInfo, null, annotation.name());
    }

    private static void processRef(final DeploymentUnit unit, final String type, final WSRefAnnotationWrapper annotation, final ClassInfo classInfo, final InjectionTarget injectionTarget, final String bindingName) throws DeploymentUnitProcessingException {

        final EEModuleDescription moduleDescription = unit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final AnnotatedElement target = createAnnotatedElement(unit, classInfo, injectionTarget);
        final String componentClassName = classInfo.name().toString();
        final Map<String, String> bindingMap = new HashMap<String, String>();
        boolean isEJB = false;
        for (final ComponentDescription componentDescription : moduleDescription.getComponentsByClassName(componentClassName)) {
            if (componentDescription instanceof SessionBeanComponentDescription) {
                isEJB = true;
                bindingMap.put(componentDescription.getComponentName() + "/" + bindingName, bindingName);
            }
        }
        if (!isEJB) {
            bindingMap.put(bindingName, bindingName);
        }

        for (String refKey : bindingMap.keySet()) {
            String refName = bindingMap.get(refKey);
            ManagedReferenceFactory factory = WebServiceReferences.createWebServiceFactory(unit, type, annotation, target, refName, refKey);
            final EEModuleClassDescription classDescription = moduleDescription.addOrGetLocalClassDescription(classInfo.name().toString());
            // Create the binding from whence our injection comes.
            final InjectionSource serviceRefSource = new FixedInjectionSource(factory, factory);
            final BindingConfiguration bindingConfiguration = new BindingConfiguration(refName, serviceRefSource);
            classDescription.getBindingConfigurations().add(bindingConfiguration);
            // our injection comes from the local lookup, no matter what.
            final ResourceInjectionConfiguration injectionConfiguration = injectionTarget != null ?
                    new ResourceInjectionConfiguration(injectionTarget, new LookupInjectionSource(refName)) : null;
            if (injectionConfiguration != null) {
                classDescription.addResourceInjection(injectionConfiguration);
            }
        }

    }

    private static AnnotatedElement createAnnotatedElement(final DeploymentUnit unit, final ClassInfo classInfo, final InjectionTarget injectionTarget) throws DeploymentUnitProcessingException {
        if (injectionTarget == null) {
            return elementForInjectionTarget(unit, classInfo);
        } else {
            return elementForInjectionTarget(unit, injectionTarget);
        }
    }


    private static AnnotatedElement elementForInjectionTarget(final DeploymentUnit unit, final ClassInfo classInfo) throws DeploymentUnitProcessingException {

        final Class<?> target = getClass(unit, classInfo.name().toString());
        return target;
    }

    private static AnnotatedElement elementForInjectionTarget(final DeploymentUnit unit, final InjectionTarget injectionTarget) throws DeploymentUnitProcessingException {
        final Module module = unit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final DeploymentReflectionIndex deploymentReflectionIndex = unit.getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);
        final String injectionTargetClassName = injectionTarget.getClassName();
        final String injectionTargetName = getInjectionTargetName(injectionTarget);
        final AccessibleObject fieldOrMethod = getInjectionTarget(injectionTargetClassName, injectionTargetName, module.getClassLoader(), deploymentReflectionIndex);
        return fieldOrMethod;
    }

    private static String getInjectionTargetName(final InjectionTarget injectionTarget) {
        final String name = injectionTarget.getName();
        if (injectionTarget instanceof FieldInjectionTarget) {
            return name;
        } else if (injectionTarget instanceof MethodInjectionTarget) {
            return name.substring(3, 4).toUpperCase(Locale.ENGLISH) + name.substring(4);
        }
        throw new UnsupportedOperationException();
    }


    private static Class<?> getClass(final DeploymentUnit du, final String className) throws DeploymentUnitProcessingException { // TODO: refactor to common code
        if (!isEmpty(className)) {
            try {
                return ClassLoadingUtils.loadClass(className, du);
            } catch (ClassNotFoundException e) {
                throw new DeploymentUnitProcessingException(e);
            }
        }

        return null;
    }

    private static boolean isEmpty(final String string) { // TODO: some common class - StringUtils ?
        return string == null || string.isEmpty();
    }

}
