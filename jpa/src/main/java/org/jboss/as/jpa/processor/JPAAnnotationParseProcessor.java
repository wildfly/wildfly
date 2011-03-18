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

import org.jboss.as.ee.component.AbstractComponentDescription;
import org.jboss.as.ee.component.BindingDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.InjectionTargetDescription;
import org.jboss.as.ee.component.ServiceBindingSourceDescription;
import org.jboss.as.jpa.container.PersistenceUnitSearch;
import org.jboss.as.jpa.service.PersistenceContextInjectorService;
import org.jboss.as.jpa.service.PersistenceUnitInjectorService;
import org.jboss.as.jpa.service.PersistenceUnitService;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import java.util.List;

/**
 * Handle PersistenceContext and PersistenceUnit annotations.
 *
 * TODO: This should iterate over components looking for annotations, not the other way around (by extending AbstractComponentConfigProcessor)
 *
 * @author Scott Marlow (based on ResourceInjectionAnnotationParsingProcessor)
 */
public class JPAAnnotationParseProcessor
    implements DeploymentUnitProcessor {

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
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);
        final String applicationName = deploymentUnit.getParent() == null ? deploymentUnit.getName() : deploymentUnit.getParent().getName();
        final CompositeIndex compositeIndex = deploymentUnit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (compositeIndex == null) {
            return;
        }

        addBindings(deploymentUnit, PERSISTENCE_CONTEXT_ANNOTATION_NAME, compositeIndex, moduleDescription, applicationName, phaseContext);

        addBindings(deploymentUnit, PERSISTENCE_UNIT_ANNOTATION_NAME, compositeIndex, moduleDescription, applicationName, phaseContext);

    }

    private void addBindings(final DeploymentUnit deploymentUnit,
                             final DotName annotationName,
                             final CompositeIndex compositeIndex,
                             final EEModuleDescription moduleDescription,
                             final String applicationName,
                             DeploymentPhaseContext phaseContext)
        throws DeploymentUnitProcessingException {

        final List<AnnotationInstance> instances = compositeIndex.getAnnotations(annotationName);
        if (instances != null) {
            for (AnnotationInstance instance : instances) {
                final BindingDescription binding = getResourceConfiguration(deploymentUnit, instance, phaseContext);
                final String componentName = getComponentName(instance);
                final AbstractComponentDescription componentDescription = moduleDescription.getComponentByClassName(componentName);

                // give hopefully enough information if component isn't found
                if (null == componentDescription) {
                    //if the component is not found it is probably a CDI component
                    //so just return
                    continue;
                }

                componentDescription.getBindings().add(binding);
            }
        }

    }

    private String getComponentName(AnnotationInstance annotation) {
        String name;
        final AnnotationTarget annotationTarget = annotation.target();
        if (annotationTarget instanceof FieldInfo) {
            name = ((FieldInfo) annotationTarget).declaringClass().name().toString();
        } else if (annotationTarget instanceof MethodInfo) {
            name = ((MethodInfo) annotationTarget).declaringClass().name().toString();
        } else if (annotationTarget instanceof ClassInfo) {
            name = ((ClassInfo) annotationTarget).name().toString();
        } else {
            // Don't expect to see this error thrown
            throw new RuntimeException("unexpected error: AnnotationTarget class of type (" + annotationTarget.getClass().getName() + ") is not handled.");
        }
        return name;
    }

    public void undeploy(DeploymentUnit context) {
    }

    private BindingDescription getResourceConfiguration(final DeploymentUnit deploymentUnit,
                                                        final AnnotationInstance annotation,
                                                        final DeploymentPhaseContext phaseContext)
        throws DeploymentUnitProcessingException {

        final AnnotationTarget annotationTarget = annotation.target();
        final BindingDescription resourceConfiguration;
        if (annotationTarget instanceof FieldInfo) {
            resourceConfiguration = processFieldResource(deploymentUnit, annotation, FieldInfo.class.cast(annotationTarget), phaseContext);
        } else if (annotationTarget instanceof MethodInfo) {
            resourceConfiguration = processMethodResource(deploymentUnit, annotation, MethodInfo.class.cast(annotationTarget), phaseContext);
        } else if (annotationTarget instanceof ClassInfo) {
            resourceConfiguration = processClassResource(deploymentUnit, annotation, ClassInfo.class.cast(annotationTarget), phaseContext);
        } else {
            resourceConfiguration = null;
        }
        return resourceConfiguration;
    }

    private BindingDescription processFieldResource(final DeploymentUnit deploymentUnit,
                                                    final AnnotationInstance annotation,
                                                    final FieldInfo fieldInfo,
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

        BindingDescription bindingDescription = new BindingDescription();
        bindingDescription.setDependency(true);
        bindingDescription.setBindingName(localContextName);
        final String injectionTypeName = injectionType.toString();
        bindingDescription.setBindingType(injectionTypeName);

        ServiceName injectorName = getInjectorServiceName(deploymentUnit, annotation, phaseContext);

        bindingDescription.setReferenceSourceDescription(new ServiceBindingSourceDescription(injectorName));

        // setup the injection target
        final InjectionTargetDescription targetDescription = new InjectionTargetDescription();
        targetDescription.setName(fieldName);
        targetDescription.setClassName(fieldInfo.declaringClass().name().toString());
        targetDescription.setType(InjectionTargetDescription.Type.FIELD);
        targetDescription.setValueClassName(injectionTypeName);
        bindingDescription.getInjectionTargetDescriptions().add(targetDescription);

        return bindingDescription;
    }

    private BindingDescription processMethodResource(final DeploymentUnit deploymentUnit,
                                                     final AnnotationInstance annotation,
                                                     final MethodInfo methodInfo,
                                                     final DeploymentPhaseContext phaseContext)
        throws DeploymentUnitProcessingException {

        final String methodName = methodInfo.name();
        if (!methodName.startsWith("set") || methodInfo.args().length != 1) {
            throw new IllegalArgumentException("@PersistenceContext injection target is invalid.  Only setter methods are allowed: " + methodInfo);
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
        final BindingDescription bindingDescription = new BindingDescription();
        bindingDescription.setDependency(true);
        bindingDescription.setBindingName(localContextName);
        final String injectionTypeName = injectionType.toString();
        bindingDescription.setBindingType(injectionTypeName);

        ServiceName injectorName = getInjectorServiceName(deploymentUnit, annotation, phaseContext);

        bindingDescription.setReferenceSourceDescription(new ServiceBindingSourceDescription(injectorName));

        // setup the injection target
        final InjectionTargetDescription targetDescription = new InjectionTargetDescription();
        targetDescription.setName(methodName);
        targetDescription.setClassName(methodInfo.declaringClass().name().toString());
        targetDescription.setType(InjectionTargetDescription.Type.METHOD);
        targetDescription.setValueClassName(injectionTypeName);
        bindingDescription.getInjectionTargetDescriptions().add(targetDescription);
        return bindingDescription;
    }

    private BindingDescription processClassResource(final DeploymentUnit deploymentUnit,
                                                    final AnnotationInstance annotation,
                                                    final ClassInfo classInfo,
                                                    final DeploymentPhaseContext phaseContext)
        throws DeploymentUnitProcessingException {

        final AnnotationValue nameValue = annotation.value("name");
        if (nameValue == null || nameValue.asString().isEmpty()) {
            throw new IllegalArgumentException("Class level @PersistenceContext annotations must provide a name.");
        }
        final String name = nameValue.asString();

        final String type = classInfo.name().toString();
        final BindingDescription bindingDescription = new BindingDescription();
        bindingDescription.setDependency(true);
        bindingDescription.setBindingName(name);
        bindingDescription.setBindingType(type);

        ServiceName injectorName = getInjectorServiceName(deploymentUnit, annotation, phaseContext);

        bindingDescription.setReferenceSourceDescription(new ServiceBindingSourceDescription(injectorName));
        return bindingDescription;
    }



    private ServiceName getInjectorServiceName(final DeploymentUnit deploymentUnit, final AnnotationInstance annotation, DeploymentPhaseContext phaseContext)
        throws DeploymentUnitProcessingException {
        String name = annotation.target().toString();  // TODO: come up with a better name
        String scopedPuName = getScopedPuName(deploymentUnit, annotation);
        ServiceName puServiceName = getPuServiceName(scopedPuName);
        ServiceName injectorName = ServiceName.of(puServiceName, name);
        if (isPersistenceContext(annotation)) {
            phaseContext.getServiceTarget().addService(injectorName, new PersistenceContextInjectorService(annotation, puServiceName, deploymentUnit, scopedPuName))
            .addDependency(puServiceName)
            .setInitialMode(ServiceController.Mode.ACTIVE)
            .install();
        }
        else {
            phaseContext.getServiceTarget().addService(injectorName, new PersistenceUnitInjectorService(annotation, puServiceName, deploymentUnit, scopedPuName))
            .addDependency(puServiceName)
            .setInitialMode(ServiceController.Mode.ACTIVE)
            .install();

        }
        return injectorName;
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

}

