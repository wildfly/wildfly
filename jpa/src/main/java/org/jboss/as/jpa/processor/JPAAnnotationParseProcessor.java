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

package org.jboss.as.jpa.processor;

import org.jboss.as.ee.component.AbstractComponentConfigProcessor;
import org.jboss.as.ee.component.AbstractComponentDescription;
import org.jboss.as.ee.component.BindingDescription;
import org.jboss.as.ee.component.BindingSourceDescription;
import org.jboss.as.ee.component.InjectionTargetDescription;
import org.jboss.as.ee.component.InterceptorDescription;
import org.jboss.as.ejb3.component.stateful.StatefulComponentDescription;
import org.jboss.as.jpa.container.PersistenceUnitSearch;
import org.jboss.as.jpa.interceptor.SFSBCreateInterceptor;
import org.jboss.as.jpa.interceptor.SFSBDestroyInterceptor;
import org.jboss.as.jpa.interceptor.SFSBInvocationInterceptorFactory;
import org.jboss.as.jpa.service.PersistenceContextBindingSourceDescription;
import org.jboss.as.jpa.service.PersistenceUnitBindingSourceDescription;
import org.jboss.as.jpa.service.PersistenceUnitService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.msc.service.ServiceName;

import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Handle PersistenceContext and PersistenceUnit annotations.
 *
 * @author Scott Marlow (based on ResourceInjectionAnnotationParsingProcessor)
 */
public class JPAAnnotationParseProcessor extends AbstractComponentConfigProcessor {


    private static final DotName PERSISTENCE_CONTEXT_ANNOTATION_NAME = DotName.createSimple(PersistenceContext.class.getName());
    private static final DotName PERSISTENCE_UNIT_ANNOTATION_NAME = DotName.createSimple(PersistenceUnit.class.getName());

    /**
     * Check the deployment annotation index for all classes with the @PersistenceContext annotation.  For each class with the
     * annotation, collect all the required information to create a managed bean instance, and attach it to the context.
     *
     * @param phaseContext the deployment unit context
     * @throws org.jboss.as.server.deployment.DeploymentUnitProcessingException
     *
     */
    protected void processComponentConfig(final DeploymentUnit deploymentUnit,
                                          final DeploymentPhaseContext phaseContext,
                                          final CompositeIndex compositeIndex,
                                          final AbstractComponentDescription componentDescription)
        throws DeploymentUnitProcessingException {

        final ClassInfo classInfo = compositeIndex.getClassByName(DotName.createSimple(componentDescription.getComponentClassName()));
        if (classInfo == null) {
            return; // We can't continue without the annotation index info.
        }
        componentDescription.addAnnotationBindings(getConfigurations(deploymentUnit, classInfo, componentDescription, phaseContext));
        final Collection<InterceptorDescription> interceptorConfigurations = componentDescription.getAllInterceptors().values();
        for (InterceptorDescription interceptorConfiguration : interceptorConfigurations) {
            final ClassInfo interceptorClassInfo = compositeIndex.getClassByName(DotName.createSimple(interceptorConfiguration.getInterceptorClassName()));
            if (interceptorClassInfo == null) {
                continue;
            }
            componentDescription.addAnnotationBindings(getConfigurations(deploymentUnit, interceptorClassInfo, componentDescription, phaseContext));
        }
    }

    private List<BindingDescription> getConfigurations(final DeploymentUnit deploymentUnit,
                                                       final ClassInfo classInfo,
                                                       final AbstractComponentDescription componentDescription,
                                                       final DeploymentPhaseContext phaseContext)
        throws DeploymentUnitProcessingException {

        final List<BindingDescription> configurations = new ArrayList<BindingDescription>();
        boolean isJPADeploymentMarker = false;
        final Map<DotName, List<AnnotationInstance>> classAnnotations = classInfo.annotations();
        if (classAnnotations != null) {
            List<AnnotationInstance> resourceAnnotations = classAnnotations.get(PERSISTENCE_UNIT_ANNOTATION_NAME);
            if (resourceAnnotations != null && resourceAnnotations.size() > 0) {
                isJPADeploymentMarker = true;
                for (AnnotationInstance annotation : resourceAnnotations) {
                    configurations.add(getConfiguration(deploymentUnit, annotation, componentDescription, phaseContext));
                }
            }
            resourceAnnotations = classAnnotations.get(PERSISTENCE_CONTEXT_ANNOTATION_NAME);
            if (resourceAnnotations != null && resourceAnnotations.size() > 0) {
                isJPADeploymentMarker = true;
                for (AnnotationInstance annotation : resourceAnnotations) {
                    configurations.add(getConfiguration(deploymentUnit, annotation, componentDescription, phaseContext));
                }
            }
        }

        if (isJPADeploymentMarker) {
            JPADeploymentMarker.mark(deploymentUnit);
        }

        return configurations;
    }

    private BindingDescription getConfiguration(final DeploymentUnit deploymentUnit,
                                                final AnnotationInstance annotation,
                                                final AbstractComponentDescription componentDescription,
                                                final DeploymentPhaseContext phaseContext)
        throws DeploymentUnitProcessingException {

        final AnnotationTarget annotationTarget = annotation.target();
        final BindingDescription resourceConfiguration;
        registerInterceptors(componentDescription, annotation);
        if (annotationTarget instanceof FieldInfo) {
            resourceConfiguration = processField(deploymentUnit, annotation, FieldInfo.class.cast(annotationTarget), componentDescription, phaseContext);
        } else if (annotationTarget instanceof MethodInfo) {
            resourceConfiguration = processMethod(deploymentUnit, annotation, MethodInfo.class.cast(annotationTarget), componentDescription, phaseContext);
        } else if (annotationTarget instanceof ClassInfo) {
            resourceConfiguration = processClassResource(deploymentUnit, annotation, ClassInfo.class.cast(annotationTarget), componentDescription, phaseContext);
        } else {
            resourceConfiguration = null;
        }
        return resourceConfiguration;
    }

    private BindingDescription processField(final DeploymentUnit deploymentUnit,
                                            final AnnotationInstance annotation,
                                            final FieldInfo fieldInfo,
                                            final AbstractComponentDescription componentDescription,
                                            final DeploymentPhaseContext phaseContext)
        throws DeploymentUnitProcessingException {

        final String fieldName = fieldInfo.name();
        final AnnotationValue declaredNameValue = annotation.value("name");
        final String declaredName = declaredNameValue != null ? declaredNameValue.asString() : null;
        final String localContextName;
        if (declaredName == null || declaredName.isEmpty()) {
            localContextName = "java:comp/env/persistence" + "/" + fieldName;
        } else {
            localContextName = declaredName;
        }

        //final AnnotationValue declaredTypeValue = annotation.value("type");
        final DotName declaredType = fieldInfo.type().name();
        final DotName injectionType = declaredType == null || declaredType.toString().equals(Object.class.getName()) ? fieldInfo.type().name() : declaredType;

        BindingDescription bindingDescription = new BindingDescription(localContextName,componentDescription);
        bindingDescription.setDependency(true);
        final String injectionTypeName = injectionType.toString();
        bindingDescription.setBindingType(injectionTypeName);

        bindingDescription.setReferenceSourceDescription(getBindingSource(deploymentUnit,annotation,injectionTypeName));

        // setup the injection target
        final InjectionTargetDescription targetDescription = new InjectionTargetDescription();
        targetDescription.setName(fieldName);
        targetDescription.setClassName(fieldInfo.declaringClass().name().toString());
        targetDescription.setType(InjectionTargetDescription.Type.FIELD);
        targetDescription.setValueClassName(injectionTypeName);
        bindingDescription.getInjectionTargetDescriptions().add(targetDescription);

        return bindingDescription;
    }

    private BindingDescription processMethod(final DeploymentUnit deploymentUnit,
                                             final AnnotationInstance annotation,
                                             final MethodInfo methodInfo,
                                             final AbstractComponentDescription componentDescription,
                                             final DeploymentPhaseContext phaseContext)
        throws DeploymentUnitProcessingException {

        final String methodName = methodInfo.name();
        if (!methodName.startsWith("set") || methodInfo.args().length != 1) {
            throw new IllegalArgumentException("injection target is invalid.  Only setter methods are allowed: " + methodInfo);
        }

        final String contextNameSuffix = methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
        final AnnotationValue declaredNameValue = annotation.value("name");
        final String declaredName = declaredNameValue != null ? declaredNameValue.asString() : null;
        final String localContextName;
        if (declaredName == null || declaredName.isEmpty()) {
            localContextName = methodInfo.declaringClass().name().toString() + "/" + contextNameSuffix;
        } else {
            localContextName = declaredName;
        }

        final DotName declaredType = methodInfo.returnType().name();
        final DotName injectionType = declaredType == null || declaredType.toString().equals(Object.class.getName()) ? methodInfo.returnType().name() : declaredType;
        final BindingDescription bindingDescription = new BindingDescription(localContextName,componentDescription);
        bindingDescription.setDependency(true);
        final String injectionTypeName = injectionType.toString();
        bindingDescription.setBindingType(injectionTypeName);

        bindingDescription.setReferenceSourceDescription(getBindingSource(deploymentUnit,annotation,injectionTypeName));

        // setup the injection target
        final InjectionTargetDescription targetDescription = new InjectionTargetDescription();
        targetDescription.setName(methodName);
        targetDescription.setClassName(methodInfo.declaringClass().name().toString());
        targetDescription.setType(InjectionTargetDescription.Type.METHOD);
        targetDescription.setValueClassName(injectionTypeName);
        bindingDescription.getInjectionTargetDescriptions().add(targetDescription);
        return bindingDescription;
    }

    private BindingDescription processClassResource(
        final DeploymentUnit deploymentUnit,
        final AnnotationInstance annotation,
        final ClassInfo classInfo,
        final AbstractComponentDescription componentDescription,
        final DeploymentPhaseContext phaseContext)
        throws DeploymentUnitProcessingException {

        final AnnotationValue nameValue = annotation.value("name");
        if (nameValue == null || nameValue.asString().isEmpty()) {
            throw new IllegalArgumentException("Class level annotations must provide a name.");
        }
        final String name = nameValue.asString();
        final String type = classInfo.name().toString();
        final BindingDescription bindingDescription = new BindingDescription(name,componentDescription);
        bindingDescription.setDependency(true);
        bindingDescription.setBindingType(type);
        bindingDescription.setReferenceSourceDescription(getBindingSource(deploymentUnit,annotation,type));
        return bindingDescription;
    }

    private BindingSourceDescription getBindingSource(
        final DeploymentUnit deploymentUnit,
        final AnnotationInstance annotation,
        String injectionTypeName)
        throws DeploymentUnitProcessingException {

        String scopedPuName = getScopedPuName(deploymentUnit, annotation);
        ServiceName puServiceName = getPuServiceName(scopedPuName);
        if (isPersistenceContext(annotation)) {
            return new PersistenceContextBindingSourceDescription(annotation, puServiceName, deploymentUnit, scopedPuName, injectionTypeName);
        } else {
            return new PersistenceUnitBindingSourceDescription(puServiceName, deploymentUnit, scopedPuName, injectionTypeName);
        }
    }

    private boolean isExtendedPersistenceContext(final AnnotationInstance annotation) {
        AnnotationValue value = annotation.value("type");
        return annotation.name().local().equals("PersistenceContext") &&
            (value != null && PersistenceContextType.EXTENDED.name().equals(value.asString()));

    }

    private boolean isPersistenceContext(final AnnotationInstance annotation) {
        return annotation.name().local().equals("PersistenceContext");
    }

    private String getScopedPuName(final DeploymentUnit deploymentUnit, final AnnotationInstance annotation)
        throws DeploymentUnitProcessingException {

        final AnnotationValue puName = annotation.value("unitName");
        String scopedPuName;
        String searchName = null;   // note:  a null searchName will match the first PU definition found

        if (puName != null) {
            searchName = puName.asString();
        }
        scopedPuName = PersistenceUnitSearch.resolvePersistenceUnitSupplier(deploymentUnit, searchName);
        if (null == scopedPuName) {
            throw new DeploymentUnitProcessingException("Can't find a deployment unit named " + puName.asString() + " at " + deploymentUnit);
        }
        return scopedPuName;
    }

    private ServiceName getPuServiceName(String scopedPuName)
        throws DeploymentUnitProcessingException {

        return PersistenceUnitService.getPUServiceName(scopedPuName);
    }

    // Register our listeners on SFSB that will be created
    private void registerInterceptors(AbstractComponentDescription componentDescription, AnnotationInstance annotation) {
        if (componentDescription instanceof StatefulComponentDescription && isExtendedPersistenceContext(annotation)) {
            componentDescription.addPostConstructComponentLifecycle(new SFSBCreateInterceptor());
            componentDescription.addPreDestroyComponentLifecycle(new SFSBDestroyInterceptor());
            componentDescription.addInterceptorFactory(SFSBInvocationInterceptorFactory.getInstance());
        }
    }

}

