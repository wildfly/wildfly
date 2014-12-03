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

package org.jboss.as.ee.component.deployers;

import org.jboss.as.ee.logging.EeLogger;
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
import org.jboss.as.ee.structure.EJBAnnotationPropertyReplacement;
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
import org.jboss.metadata.property.PropertyReplacer;
import org.jboss.modules.Module;

import javax.annotation.Resource;
import javax.annotation.Resources;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Deployment processor responsible for analyzing each attached {@link org.jboss.as.ee.component.ComponentDescription} instance to configure
 * required resource injection configurations.
 *
 * @author John Bailey
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author Eduardo Martins
 */
public class ResourceInjectionAnnotationParsingProcessor implements DeploymentUnitProcessor {

    private static final DotName RESOURCE_ANNOTATION_NAME = DotName.createSimple(Resource.class.getName());
    private static final DotName RESOURCES_ANNOTATION_NAME = DotName.createSimple(Resources.class.getName());
    private static final String JAVAX_NAMING_CONTEXT = "javax.naming.Context";
    public static final Map<String, String> FIXED_LOCATIONS;
    public static final Set<String> SIMPLE_ENTRIES;

    static {
        final Map<String, String> locations = new HashMap<String, String>();
        locations.put("javax.transaction.UserTransaction", "java:jboss/UserTransaction");
        locations.put("javax.transaction.TransactionSynchronizationRegistry", "java:jboss/TransactionSynchronizationRegistry");

        //we have to be careful with java:comp lookups here
        //as they will not work in entries in application.xml, as there is no comp context available
        //so we can only use it for resources that are not valid to be entries in application.xml
        locations.put("javax.enterprise.inject.spi.BeanManager", "java:comp/BeanManager");
        locations.put("javax.ejb.TimerService", "java:comp/TimerService");
        locations.put("org.omg.CORBA.ORB", "java:comp/ORB");
        FIXED_LOCATIONS = Collections.unmodifiableMap(locations);

        final Set<String> simpleEntries = new HashSet<String>();
        simpleEntries.add("boolean");
        simpleEntries.add("char");
        simpleEntries.add("byte");
        simpleEntries.add("short");
        simpleEntries.add("int");
        simpleEntries.add("long");
        simpleEntries.add("double");
        simpleEntries.add("float");
        simpleEntries.add("java.lang.Boolean");
        simpleEntries.add("java.lang.Character");
        simpleEntries.add("java.lang.Byte");
        simpleEntries.add("java.lang.Short");
        simpleEntries.add("java.lang.Integer");
        simpleEntries.add("java.lang.Long");
        simpleEntries.add("java.lang.Double");
        simpleEntries.add("java.lang.Float");
        simpleEntries.add("java.lang.String");
        simpleEntries.add("java.lang.Class");
        SIMPLE_ENTRIES = Collections.unmodifiableSet(simpleEntries);
    }


    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final CompositeIndex index = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX);
        final EEApplicationClasses applicationClasses = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final PropertyReplacer replacer = EJBAnnotationPropertyReplacement.propertyReplacer(deploymentUnit);
        if (module == null) {
            return;
        }
        final List<AnnotationInstance> resourceAnnotations = index.getAnnotations(RESOURCE_ANNOTATION_NAME);
        for (AnnotationInstance annotation : resourceAnnotations) {
            final AnnotationTarget annotationTarget = annotation.target();
            final AnnotationValue nameValue = annotation.value("name");
            final String name = (nameValue != null) ? replacer.replaceProperties(nameValue.asString()) : null;
            final AnnotationValue typeValue = annotation.value("type");
            final String type = typeValue != null ? typeValue.asClass().name().toString() : null;
            if (annotationTarget instanceof FieldInfo) {
                final FieldInfo fieldInfo = (FieldInfo) annotationTarget;
                final ClassInfo classInfo = fieldInfo.declaringClass();
                EEModuleClassDescription classDescription = eeModuleDescription.addOrGetLocalClassDescription(classInfo.name().toString());
                processFieldResource(phaseContext, fieldInfo, name, type, classDescription, annotation, eeModuleDescription, module, applicationClasses, replacer);
            } else if (annotationTarget instanceof MethodInfo) {
                final MethodInfo methodInfo = (MethodInfo) annotationTarget;
                ClassInfo classInfo = methodInfo.declaringClass();
                EEModuleClassDescription classDescription = eeModuleDescription.addOrGetLocalClassDescription(classInfo.name().toString());
                processMethodResource(phaseContext, methodInfo, name, type, classDescription, annotation, eeModuleDescription, module, applicationClasses, replacer);
            } else if (annotationTarget instanceof ClassInfo) {
                final ClassInfo classInfo = (ClassInfo) annotationTarget;
                EEModuleClassDescription classDescription = eeModuleDescription.addOrGetLocalClassDescription(classInfo.name().toString());
                processClassResource(phaseContext, name, type, classDescription, annotation, eeModuleDescription, module, applicationClasses, replacer);
            }
        }
        final List<AnnotationInstance> resourcesAnnotations = index.getAnnotations(RESOURCES_ANNOTATION_NAME);
        for (AnnotationInstance outerAnnotation : resourcesAnnotations) {
            final AnnotationTarget annotationTarget = outerAnnotation.target();
            if (annotationTarget instanceof ClassInfo) {
                final ClassInfo classInfo = (ClassInfo) annotationTarget;
                final AnnotationInstance[] values = outerAnnotation.value("value").asNestedArray();
                for (AnnotationInstance annotation : values) {
                    final AnnotationValue nameValue = annotation.value("name");
                    final String name = (nameValue != null) ? replacer.replaceProperties(nameValue.asString()) : null;
                    final AnnotationValue typeValue = annotation.value("type");
                    final String type = (typeValue != null) ? typeValue.asClass().name().toString() : null;
                    EEModuleClassDescription classDescription = eeModuleDescription.addOrGetLocalClassDescription(classInfo.name().toString());
                    processClassResource(phaseContext, name, type, classDescription, annotation, eeModuleDescription, module, applicationClasses, replacer);
                }
            }
        }
    }

    public void undeploy(final DeploymentUnit context) {
    }

    protected void processFieldResource(final DeploymentPhaseContext phaseContext, final FieldInfo fieldInfo, final String name, final String type, final EEModuleClassDescription classDescription, final AnnotationInstance annotation, final EEModuleDescription eeModuleDescription, final Module module, final EEApplicationClasses applicationClasses, final PropertyReplacer replacer) throws DeploymentUnitProcessingException {
        final String fieldName = fieldInfo.name();
        final String injectionType = isEmpty(type) || type.equals(Object.class.getName()) ? fieldInfo.type().name().toString() : type;
        final String localContextName = isEmpty(name) ? fieldInfo.declaringClass().name().toString() + "/" + fieldName : name;
        final InjectionTarget targetDescription = new FieldInjectionTarget(fieldInfo.declaringClass().name().toString(), fieldName, fieldInfo.type().name().toString());
        process(phaseContext, classDescription, annotation, injectionType, localContextName, targetDescription, eeModuleDescription, module, applicationClasses, replacer);
    }

    protected void processMethodResource(final DeploymentPhaseContext phaseContext, final MethodInfo methodInfo, final String name, final String type, final EEModuleClassDescription classDescription, final AnnotationInstance annotation, final EEModuleDescription eeModuleDescription, final Module module, final EEApplicationClasses applicationClasses, final PropertyReplacer replacer) throws DeploymentUnitProcessingException {
        final String methodName = methodInfo.name();
        if (!methodName.startsWith("set") || methodInfo.args().length != 1) {
            throw EeLogger.ROOT_LOGGER.setterMethodOnly("@Resource", methodInfo);
        }

        final String contextNameSuffix = methodName.substring(3, 4).toLowerCase(Locale.ENGLISH) + methodName.substring(4);
        final String localContextName = isEmpty(name) ? methodInfo.declaringClass().name().toString() + "/" + contextNameSuffix : name;

        final String injectionType = isEmpty(type) || type.equals(Object.class.getName()) ? methodInfo.args()[0].name().toString() : type;
        final InjectionTarget targetDescription = new MethodInjectionTarget(methodInfo.declaringClass().name().toString(), methodName, methodInfo.args()[0].name().toString());
        process(phaseContext, classDescription, annotation, injectionType, localContextName, targetDescription, eeModuleDescription, module, applicationClasses, replacer);
    }

    protected void processClassResource(final DeploymentPhaseContext phaseContext, final String name, final String type, final EEModuleClassDescription classDescription, final AnnotationInstance annotation, final EEModuleDescription eeModuleDescription, final Module module, final EEApplicationClasses applicationClasses, final PropertyReplacer replacer) throws DeploymentUnitProcessingException {
        if (isEmpty(name)) {
            throw EeLogger.ROOT_LOGGER.annotationAttributeMissing("@Resource", "name");
        }
        final String realType;
        if (isEmpty(type)) {
            realType = Object.class.getName();
        } else {
            realType = type;
        }
        process(phaseContext, classDescription, annotation, realType, name, null, eeModuleDescription, module, applicationClasses, replacer);
    }

    protected void process(final DeploymentPhaseContext phaseContext, final EEModuleClassDescription classDescription, final AnnotationInstance annotation, final String injectionType, final String localContextName, final InjectionTarget targetDescription, final EEModuleDescription eeModuleDescription, final Module module, final EEApplicationClasses applicationClasses, final PropertyReplacer replacer) throws DeploymentUnitProcessingException {
        final EEResourceReferenceProcessorRegistry registry = phaseContext.getDeploymentUnit().getAttachment(Attachments.RESOURCE_REFERENCE_PROCESSOR_REGISTRY);
        final AnnotationValue lookupAnnotation = annotation.value("lookup");
        String lookup = (lookupAnnotation == null) ? null : replacer.replaceProperties(lookupAnnotation.asString());
        // if "lookup" hasn't been specified then fallback on "mappedName" which we treat the same as "lookup"
        if (isEmpty(lookup)) {
            final AnnotationValue mappedNameAnnotationValue = annotation.value("mappedName");
            lookup = (mappedNameAnnotationValue == null) ? null : replacer.replaceProperties(mappedNameAnnotationValue.asString());
        }

        if (isEmpty(lookup) && FIXED_LOCATIONS.containsKey(injectionType)) {
            lookup = FIXED_LOCATIONS.get(injectionType);
        }
        InjectionSource valueSource = null;
        final boolean isEnvEntryType = this.isEnvEntryType(injectionType, module);
        if (!isEmpty(lookup)) {
            valueSource = new LookupInjectionSource(lookup, JAVAX_NAMING_CONTEXT.equals(injectionType));
        } else if (isEnvEntryType) {
            // if it's an env-entry type then we do *not* create a BindingConfiguration to bind to the ENC
            // since the binding (value) for env-entry is always driven from a deployment descriptor.
            // The deployment descriptor processing and subsequent binding in the ENC is taken care off by a
            // different Deployment unit processor. If the value isn't specified in the deployment descriptor,
            // then there will be no binding the ENC and that's what is expected by the Java EE 6 spec. Furthermore,
            // if the @Resource is an env-entry binding then the injection target will be optional since in the absence of
            // an env-entry-value, there won't be a binding and effectively no injection. This again is as expected by spec.
        } else {
            //otherwise we just try and handle it
            //if we don't have a value source we will try and inject from a lookup
            //and the user has to configure the value in a deployment descriptor
            final EEResourceReferenceProcessor resourceReferenceProcessor = registry.getResourceReferenceProcessor(injectionType);
            if (resourceReferenceProcessor != null) {
                valueSource = resourceReferenceProcessor.getResourceReferenceBindingSource();
            }
        }

        // EE.5.2.4
        // Each injection of an object corresponds to a JNDI lookup. Whether a new
        // instance of the requested object is injected, or whether a shared instance is
        // injected, is determined by the rules described above.

        // Because of performance we allow any type of InjectionSource.

        if (valueSource == null) {
            // the ResourceInjectionConfiguration is created by LazyResourceInjection
            if (targetDescription != null) {
                final LookupInjectionSource optionalInjection = new LookupInjectionSource(localContextName, true);
                final ResourceInjectionConfiguration injectionConfiguration = new ResourceInjectionConfiguration(targetDescription, optionalInjection, true);
                classDescription.addResourceInjection(injectionConfiguration);
            }
        } else {
            // our injection comes from the local lookup, no matter what.
            final InjectionSource injectionSource = new LookupInjectionSource(localContextName);
            final ResourceInjectionConfiguration injectionConfiguration = targetDescription != null ?
                    new ResourceInjectionConfiguration(targetDescription, injectionSource) : null;

            final BindingConfiguration bindingConfiguration = new BindingConfiguration(localContextName, valueSource);
            classDescription.getBindingConfigurations().add(bindingConfiguration);

            if (injectionConfiguration != null) {
                classDescription.addResourceInjection(injectionConfiguration);
            }
        }
    }

    private boolean isEmpty(final String string) {
        return string == null || string.isEmpty();
    }

    private boolean isEnvEntryType(final String type, final Module module) {
        if (SIMPLE_ENTRIES.contains(type)) {
            return true;
        }
        try {
            return module.getClassLoader().loadClass(type).isEnum();
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

}
