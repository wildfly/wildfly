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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.persistence.PersistenceContexts;
import javax.persistence.PersistenceUnit;
import javax.persistence.PersistenceUnits;
import javax.persistence.SynchronizationType;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.FieldInjectionTarget;
import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.ee.component.InjectionTarget;
import org.jboss.as.ee.component.LookupInjectionSource;
import org.jboss.as.ee.component.MethodInjectionTarget;
import org.jboss.as.ee.component.ResourceInjectionConfiguration;
import org.jboss.as.ee.structure.SpecDescriptorPropertyReplacement;
import org.jboss.as.jpa.config.JPADeploymentSettings;
import org.jboss.as.jpa.container.PersistenceUnitSearch;
import org.jboss.as.jpa.injectors.PersistenceContextInjectionSource;
import org.jboss.as.jpa.injectors.PersistenceUnitInjectionSource;
import org.jboss.as.jpa.messages.JpaLogger;
import org.jboss.as.jpa.service.PersistenceUnitServiceImpl;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.DeploymentUtils;
import org.jboss.as.server.deployment.JPADeploymentMarker;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.FieldInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.msc.service.ServiceName;
import org.jipijapa.plugin.spi.PersistenceUnitMetadata;

import static org.jboss.as.jpa.messages.JpaLogger.ROOT_LOGGER;

/**
 * Handle PersistenceContext and PersistenceUnit annotations.
 *
 * @author Scott Marlow (based on ResourceInjectionAnnotationParsingProcessor)
 */
public class JPAAnnotationProcessor implements DeploymentUnitProcessor {


    private static final DotName PERSISTENCE_CONTEXT_ANNOTATION_NAME = DotName.createSimple(PersistenceContext.class.getName());
    private static final DotName PERSISTENCE_CONTEXTS_ANNOTATION_NAME =  DotName.createSimple(PersistenceContexts.class.getName());
    private static final DotName PERSISTENCE_UNIT_ANNOTATION_NAME = DotName.createSimple(PersistenceUnit.class.getName());
    private static final DotName PERSISTENCE_UNITS_ANNOTATION_NAME = DotName.createSimple(PersistenceUnits.class.getName());

    private static final String ENTITY_MANAGER_CLASS = "javax.persistence.EntityManager";
    private static final String ENTITY_MANAGERFACTORY_CLASS = "javax.persistence.EntityManagerFactory";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final CompositeIndex index = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX);
        final EEApplicationClasses applicationClasses = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);

        // @PersistenceContext
        List<AnnotationInstance> persistenceContexts = index.getAnnotations(PERSISTENCE_CONTEXT_ANNOTATION_NAME);
        // create binding and injection configurations out of the @PersistenceContext annotations
        this.processPersistenceAnnotations(deploymentUnit, eeModuleDescription, persistenceContexts, applicationClasses);

        // @PersistenceContexts
        List<AnnotationInstance> collectionPersistenceContexts = index.getAnnotations(PERSISTENCE_CONTEXTS_ANNOTATION_NAME);
        // create binding and injection configurations out of the @PersistenceContext annotations
        processPersistenceAnnotations(deploymentUnit, eeModuleDescription, collectionPersistenceContexts, applicationClasses);

        // @PersistenceUnits
        List<AnnotationInstance> collectionPersistenceunits = index.getAnnotations(PERSISTENCE_UNITS_ANNOTATION_NAME);
        processPersistenceAnnotations(deploymentUnit, eeModuleDescription, collectionPersistenceunits, applicationClasses);

        // @PersistenceUnit
        List<AnnotationInstance> persistenceUnits = index.getAnnotations(PERSISTENCE_UNIT_ANNOTATION_NAME);
        // create binding and injection configurations out of the @PersistenceUnit annotations
        this.processPersistenceAnnotations(deploymentUnit, eeModuleDescription, persistenceUnits, applicationClasses);

        // if we found any @PersistenceContext or @PersistenceUnit annotations then mark this as a JPA deployment
        if (!persistenceContexts.isEmpty() || !persistenceUnits.isEmpty() ||
                !collectionPersistenceContexts.isEmpty() || !collectionPersistenceunits.isEmpty()) {
            JPADeploymentMarker.mark(deploymentUnit);
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {

    }

    private void processPersistenceAnnotations(final DeploymentUnit deploymentUnit, final EEModuleDescription eeModuleDescription, List<AnnotationInstance> persistenceContexts, final EEApplicationClasses applicationClasses) throws
        DeploymentUnitProcessingException {

        for (AnnotationInstance annotation : persistenceContexts) {
            ClassInfo declaringClass;
            final AnnotationTarget annotationTarget = annotation.target();
            if (annotationTarget instanceof FieldInfo) {
                FieldInfo fieldInfo = (FieldInfo) annotationTarget;
                declaringClass = fieldInfo.declaringClass();
                EEModuleClassDescription eeModuleClassDescription = eeModuleDescription.addOrGetLocalClassDescription(declaringClass.name().toString());
                this.processField(deploymentUnit, annotation, fieldInfo, eeModuleClassDescription);
            } else if (annotationTarget instanceof MethodInfo) {
                MethodInfo methodInfo = (MethodInfo) annotationTarget;
                declaringClass = methodInfo.declaringClass();
                EEModuleClassDescription eeModuleClassDescription = eeModuleDescription.addOrGetLocalClassDescription(declaringClass.name().toString());
                this.processMethod(deploymentUnit, annotation, methodInfo, eeModuleClassDescription);
            } else if (annotationTarget instanceof ClassInfo) {
                declaringClass = (ClassInfo) annotationTarget;
                EEModuleClassDescription eeModuleClassDescription = eeModuleDescription.addOrGetLocalClassDescription(declaringClass.name().toString());
                this.processClass(deploymentUnit, annotation, eeModuleClassDescription);
            }
        }
    }

    private void processField(final DeploymentUnit deploymentUnit, final AnnotationInstance annotation, final FieldInfo fieldInfo,
                              final EEModuleClassDescription eeModuleClassDescription) throws
        DeploymentUnitProcessingException {

        final String fieldName = fieldInfo.name();
        final AnnotationValue declaredNameValue = annotation.value("name");
        final String declaredName = declaredNameValue != null ? declaredNameValue.asString() : null;
        final String localContextName;
        if (declaredName == null || declaredName.isEmpty()) {
            localContextName = fieldInfo.declaringClass().name().toString() + "/" + fieldName;
        } else {
            localContextName = declaredName;
        }

        //final AnnotationValue declaredTypeValue = annotation.value("type");
        final DotName declaredTypeDotName = fieldInfo.type().name();
        final DotName injectionTypeDotName = declaredTypeDotName == null || declaredTypeDotName.toString().equals(Object.class.getName()) ? fieldInfo.type().name() : declaredTypeDotName;

        final String injectionType = injectionTypeDotName.toString();
        final InjectionSource bindingSource = this.getBindingSource(deploymentUnit, annotation, injectionType, eeModuleClassDescription);
        if (bindingSource != null) {
            final BindingConfiguration bindingConfiguration = new BindingConfiguration(localContextName, bindingSource);
            eeModuleClassDescription.getBindingConfigurations().add(bindingConfiguration);

            // setup the injection target
            final InjectionTarget injectionTarget = new FieldInjectionTarget(fieldInfo.declaringClass().name().toString(), fieldName, fieldInfo.type().name().toString());
            // source is always local ENC jndi
            final InjectionSource injectionSource = new LookupInjectionSource(localContextName);
            final ResourceInjectionConfiguration injectionConfiguration = new ResourceInjectionConfiguration(injectionTarget, injectionSource);
            eeModuleClassDescription.addResourceInjection(injectionConfiguration);
        }
    }

    private void processMethod(final DeploymentUnit deploymentUnit, final AnnotationInstance annotation, final MethodInfo methodInfo,
                               final EEModuleClassDescription eeModuleClassDescription) throws
        DeploymentUnitProcessingException {

        final String methodName = methodInfo.name();
        if (!methodName.startsWith("set") || methodInfo.args().length != 1) {
            eeModuleClassDescription.setInvalid(JpaLogger.ROOT_LOGGER.setterMethodOnlyAnnotation(annotation.name().toString(), methodInfo));
            return;
        }

        final String contextNameSuffix = methodName.substring(3, 4).toLowerCase(Locale.ENGLISH) + methodName.substring(4);
        final AnnotationValue declaredNameValue = annotation.value("name");
        final String declaredName = declaredNameValue != null ? declaredNameValue.asString() : null;
        final String localContextName;
        if (declaredName == null || declaredName.isEmpty()) {
            localContextName = methodInfo.declaringClass().name().toString() + "/" + contextNameSuffix;
        } else {
            localContextName = declaredName;
        }

        final String injectionType = methodInfo.args()[0].name().toString();
        final InjectionSource bindingSource = this.getBindingSource(deploymentUnit, annotation, injectionType, eeModuleClassDescription);
        if (bindingSource != null) {
            final BindingConfiguration bindingConfiguration = new BindingConfiguration(localContextName, bindingSource);
            eeModuleClassDescription.getBindingConfigurations().add(bindingConfiguration);

            // setup the injection configuration
            final InjectionTarget injectionTarget = new MethodInjectionTarget(methodInfo.declaringClass().name().toString(), methodName, methodInfo.args()[0].name().toString());
            // source is always local ENC jndi name
            final InjectionSource injectionSource = new LookupInjectionSource(localContextName);
            final ResourceInjectionConfiguration injectionConfiguration = new ResourceInjectionConfiguration(injectionTarget, injectionSource);

            eeModuleClassDescription.addResourceInjection(injectionConfiguration);
        }
    }

    private void processClass(final DeploymentUnit deploymentUnit, final AnnotationInstance annotation,
                              final EEModuleClassDescription eeModuleClassDescription) throws
        DeploymentUnitProcessingException {

        bindClassSources(deploymentUnit, annotation, eeModuleClassDescription);
    }

    private void bindClassSources(final DeploymentUnit deploymentUnit, final AnnotationInstance annotation, final EEModuleClassDescription classDescription)
        throws DeploymentUnitProcessingException {

        // handle PersistenceContext and PersistenceUnit annotations
        if (isPersistenceContext(annotation) ||
                isPersistenceUnit(annotation)) {
            String injectionTypeName = getClassLevelInjectionType(annotation);
            InjectionSource injectionSource = getBindingSource(deploymentUnit, annotation, injectionTypeName, classDescription);
            if (injectionSource != null) {
                final AnnotationValue nameValue = annotation.value("name");
                if (nameValue == null || nameValue.asString().isEmpty()) {
                    classDescription.setInvalid(JpaLogger.ROOT_LOGGER.classLevelAnnotationParameterRequired(annotation.name().toString(), classDescription.getClassName(), "name"));
                    return;
                }
                final String name = nameValue.asString();

                final BindingConfiguration bindingConfiguration = new BindingConfiguration(name, injectionSource);
                classDescription.getBindingConfigurations().add(bindingConfiguration);
            }
        } else if (isPersistenceUnits(annotation)) {
            // handle PersistenceUnits (array of PersistenceUnit)
            AnnotationValue containedPersistenceUnits = annotation.value("value");
            AnnotationInstance[] arrayPersistenceUnits;
            if (containedPersistenceUnits != null &&
                (arrayPersistenceUnits = containedPersistenceUnits.asNestedArray()) != null) {
                for (int source = 0; source < arrayPersistenceUnits.length; source++) {
                    String injectionTypeName = getClassLevelInjectionType(arrayPersistenceUnits[source]);
                    InjectionSource injectionSource = getBindingSource(deploymentUnit, arrayPersistenceUnits[source], injectionTypeName, classDescription);
                    if (injectionSource != null) {
                        final AnnotationValue nameValue = arrayPersistenceUnits[source].value("name");
                        if (nameValue == null || nameValue.asString().isEmpty()) {
                            classDescription.setInvalid(JpaLogger.ROOT_LOGGER.classLevelAnnotationParameterRequired(arrayPersistenceUnits[source].name().toString(), classDescription.getClassName(), "name"));
                            return;
                        }
                        final String name = nameValue.asString();

                        final BindingConfiguration bindingConfiguration = new BindingConfiguration(name, injectionSource);
                        classDescription.getBindingConfigurations().add(bindingConfiguration);
                    }
                }
            }
        } else if (isPersistenceContexts(annotation)) {
            // handle PersistenceContexts (array of PersistenceContext)
            AnnotationValue containedPersistenceContexts = annotation.value("value");
            AnnotationInstance[] arrayPersistenceContexts;
            if (containedPersistenceContexts != null &&
                (arrayPersistenceContexts = containedPersistenceContexts.asNestedArray()) != null) {
                for (int source = 0; source < arrayPersistenceContexts.length; source++) {
                    String injectionTypeName = getClassLevelInjectionType(arrayPersistenceContexts[source]);
                    InjectionSource injectionSource = getBindingSource(deploymentUnit, arrayPersistenceContexts[source], injectionTypeName, classDescription);
                    if (injectionSource != null) {
                        final AnnotationValue nameValue = arrayPersistenceContexts[source].value("name");
                        if (nameValue == null || nameValue.asString().isEmpty()) {
                            classDescription.setInvalid(JpaLogger.ROOT_LOGGER.classLevelAnnotationParameterRequired(arrayPersistenceContexts[source].name().toString(), classDescription.getClassName(), "name"));
                            return;
                        }
                        final String name = nameValue.asString();

                        final BindingConfiguration bindingConfiguration = new BindingConfiguration(name, injectionSource);
                        classDescription.getBindingConfigurations().add(bindingConfiguration);
                    }
                }
            }
        }
    }

    private InjectionSource getBindingSource(final DeploymentUnit deploymentUnit, final AnnotationInstance annotation, String injectionTypeName, final EEModuleClassDescription classDescription)
        throws DeploymentUnitProcessingException {
        PersistenceUnitMetadata pu = getPersistenceUnit(deploymentUnit, annotation, classDescription);
        if (pu == null) {
            return null;
        }
        String scopedPuName = pu.getScopedPersistenceUnitName();
        ServiceName puServiceName = getPuServiceName(scopedPuName);
        if (isPersistenceContext(annotation)) {
            if (pu.getTransactionType() == PersistenceUnitTransactionType.RESOURCE_LOCAL) {
                classDescription.setInvalid(JpaLogger.ROOT_LOGGER.cannotInjectResourceLocalEntityManager());
                return null;
            }
            AnnotationValue pcType = annotation.value("type");
            PersistenceContextType type = (pcType == null || PersistenceContextType.TRANSACTION.name().equals(pcType.asString()))
                ? PersistenceContextType.TRANSACTION : PersistenceContextType.EXTENDED;

            AnnotationValue stType = annotation.value("synchronization");
            SynchronizationType synchronizationType =
                    (stType == null || SynchronizationType.SYNCHRONIZED.name().equals(stType.asString()))?
                            SynchronizationType.SYNCHRONIZED: SynchronizationType.UNSYNCHRONIZED;

            Map<String, String> properties;
            AnnotationValue value = annotation.value("properties");
            AnnotationInstance[] props = value != null ? value.asNestedArray() : null;
            if (props != null) {
                properties = new HashMap<>();
                for (int source = 0; source < props.length; source++) {
                    properties.put(props[source].value("name").asString(), props[source].value("value").asString());
                }
            } else {
                properties = null;
            }
            // get deployment settings from top level du (jboss-all.xml is only parsed at the top level).
            final JPADeploymentSettings jpaDeploymentSettings = DeploymentUtils.getTopDeploymentUnit(deploymentUnit).getAttachment(JpaAttachments.DEPLOYMENT_SETTINGS_KEY);
            return new PersistenceContextInjectionSource(type, synchronizationType , properties, puServiceName, deploymentUnit.getServiceRegistry(), scopedPuName, injectionTypeName, pu, jpaDeploymentSettings);
        } else {
            return new PersistenceUnitInjectionSource(puServiceName, deploymentUnit.getServiceRegistry(), injectionTypeName, pu);
        }
    }

    private boolean isPersistenceContext(final AnnotationInstance annotation) {
        return annotation.name().local().equals("PersistenceContext");
    }

    private boolean isPersistenceUnit(final AnnotationInstance annotation) {
        return annotation.name().local().equals("PersistenceUnit");
    }

    private boolean isPersistenceContexts(final AnnotationInstance annotation) {
        return annotation.name().local().equals("PersistenceContexts");
    }

    private boolean isPersistenceUnits(final AnnotationInstance annotation) {
        return annotation.name().local().equals("PersistenceUnits");
    }

    /**
     * Based on the the annotation type, its either entitymanager or entitymanagerfactory
     *
     * @param annotation
     * @return
     */
    private String getClassLevelInjectionType(final AnnotationInstance annotation) {
        boolean isPC = annotation.name().local().equals("PersistenceContext");
        return isPC ? ENTITY_MANAGER_CLASS : ENTITY_MANAGERFACTORY_CLASS;
    }

    private PersistenceUnitMetadata getPersistenceUnit(final DeploymentUnit deploymentUnit, final AnnotationInstance annotation, EEModuleClassDescription classDescription)
        throws DeploymentUnitProcessingException {

        final AnnotationValue puName = annotation.value("unitName");
        String searchName = null;   // note:  a null searchName will match the first PU definition found

        if (puName != null && (searchName = puName.asString()) != null) {
            searchName = SpecDescriptorPropertyReplacement.propertyReplacer(deploymentUnit).replaceProperties(searchName);
        }
        ROOT_LOGGER.debugf("persistence unit search for unitName=%s referenced from class=%s (annotation=%s)", searchName, classDescription.getClassName(), annotation.toString());
        PersistenceUnitMetadata pu = PersistenceUnitSearch.resolvePersistenceUnitSupplier(deploymentUnit, searchName);
        if (null == pu) {
            classDescription.setInvalid(JpaLogger.ROOT_LOGGER.persistenceUnitNotFound(searchName, deploymentUnit));
            return null;
        }
        return pu;
    }

    private ServiceName getPuServiceName(String scopedPuName)
        throws DeploymentUnitProcessingException {

        return PersistenceUnitServiceImpl.getPUServiceName(scopedPuName);
    }

}

