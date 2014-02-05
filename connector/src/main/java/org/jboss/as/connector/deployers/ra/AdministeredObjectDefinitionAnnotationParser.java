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

package org.jboss.as.connector.deployers.ra;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.resource.AdministeredObjectDefinition;
import javax.resource.AdministeredObjectDefinitions;

import org.jboss.as.ee.logging.EeLogger;
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleClassDescription;
import org.jboss.as.ee.component.EEModuleDescription;
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

/**
 * Deployment processor responsible for processing {@link AdministeredObjectDefinition} and {@link AdministeredObjectDefinitions}
 * and creating {@link org.jboss.as.ee.component.BindingConfiguration}s out of them
 *
 * @author Jesper Pedersen
 */
public class AdministeredObjectDefinitionAnnotationParser implements DeploymentUnitProcessor {

    private static final DotName ADMINISTERED_OBJECT_DEFINITION = DotName.createSimple(AdministeredObjectDefinition.class.getName());
    private static final DotName ADMINISTERED_OBJECT_DEFINITIONS = DotName.createSimple(AdministeredObjectDefinitions.class.getName());



    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final CompositeIndex index = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX);
        final EEApplicationClasses applicationClasses = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);

        // @AdministeredObjectDefinitions
        List<AnnotationInstance> adminObjectDefinitions = index.getAnnotations(ADMINISTERED_OBJECT_DEFINITIONS);
        if (adminObjectDefinitions != null) {
            for (AnnotationInstance annotation : adminObjectDefinitions) {
                final AnnotationTarget target = annotation.target();
                if (target instanceof ClassInfo == false) {
                    throw EeLogger.ROOT_LOGGER.classOnlyAnnotation("@AdministeredObjectDefinitions", target);
                }
                // get the nested @AdministeredObjectDefinition out of the outer @AdministeredObjectDefinitions
                List<AnnotationInstance> adminObjects = this.getNestedAdministeredObjectAnnotations(annotation);
                // process the nested @AdministeredObjectDefinition
                for (AnnotationInstance adminObject : adminObjects) {
                    // create binding configurations out of it
                    this.processAdministeredObjectDefinition(eeModuleDescription, adminObject, (ClassInfo) target, applicationClasses);
                }
            }
        }

        // @AdministeredObjectDefinition
        List<AnnotationInstance> adminObjects = index.getAnnotations(ADMINISTERED_OBJECT_DEFINITION);
        if (adminObjects != null) {
            for (AnnotationInstance adminObject : adminObjects) {
                final AnnotationTarget target = adminObject.target();
                if (target instanceof ClassInfo == false) {
                    throw EeLogger.ROOT_LOGGER.classOnlyAnnotation("@AdministeredObjectDefinition", target);
                }
                // create binding configurations out of it
                this.processAdministeredObjectDefinition(eeModuleDescription, adminObject, (ClassInfo) target, applicationClasses);
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private void processAdministeredObjectDefinition(final EEModuleDescription eeModuleDescription, final AnnotationInstance adminObjectDefinition, final ClassInfo targetClass, final EEApplicationClasses applicationClasses) throws DeploymentUnitProcessingException {
        // create BindingConfiguration out of the @AdministeredObjectDefinition annotation
        final BindingConfiguration bindingConfiguration = this.getBindingConfiguration(adminObjectDefinition);
        EEModuleClassDescription classDescription = eeModuleDescription.addOrGetLocalClassDescription(targetClass.name().toString());
        // add the binding configuration via a class configurator
        classDescription.getBindingConfigurations().add(bindingConfiguration);
    }

    private BindingConfiguration getBindingConfiguration(final AnnotationInstance adminObjectAnnotation) {

        String name = asString(adminObjectAnnotation, "name");
        if (name == null || name.isEmpty()) {
            throw EeLogger.ROOT_LOGGER.annotationAttributeMissing("@AdministeredObjectDefinition", "name");
        }

        // If the name doesn't have a namespace then it defaults to java:comp/env
        if (!name.startsWith("java:")) {
            name = "java:comp/env/" + name;
        }

        String clz = asString(adminObjectAnnotation, "className");
        if (clz == null || clz.equals(Object.class.getName())) {
            throw EeLogger.ROOT_LOGGER.annotationAttributeMissing("@AdministeredObjectDefinition", "className");
        }

        String ra = asString(adminObjectAnnotation, "resourceAdapter");
        if (ra == null || ra.equals(Object.class.getName())) {
            throw EeLogger.ROOT_LOGGER.annotationAttributeMissing("@AdministeredObjectDefinition", "resourceAdapter");
        }

        final DirectAdministeredObjectInjectionSource directAdministeredObjectInjectionSource =
            new DirectAdministeredObjectInjectionSource(name, clz, ra);

        directAdministeredObjectInjectionSource.setDescription(asString(adminObjectAnnotation,
                                                                        DirectAdministeredObjectInjectionSource.DESCRIPTION));
        directAdministeredObjectInjectionSource.setInterface(asString(adminObjectAnnotation,
                                                                      DirectAdministeredObjectInjectionSource.INTERFACE));
        directAdministeredObjectInjectionSource.setProperties(asArray(adminObjectAnnotation,
                                                                      DirectAdministeredObjectInjectionSource.PROPERTIES));

        final BindingConfiguration bindingDescription = new BindingConfiguration(name, directAdministeredObjectInjectionSource);
        return bindingDescription;
    }

    private String asString(final AnnotationInstance annotation, String property) {
        AnnotationValue value = annotation.value(property);
        return value == null ? null : value.asString();
    }

    private String[] asArray(final AnnotationInstance annotation, String property) {
        AnnotationValue value = annotation.value(property);
        return value == null ? null : value.asStringArray();
    }

    /**
     * Returns the nested {@link AdministeredObjectDefinition} annotations out of the outer {@link AdministeredObjectDefinitions} annotation
     *
     * @param adminObjectDefinitions The outer {@link AdministeredObjectDefinitions} annotation
     * @return
     */
    private List<AnnotationInstance> getNestedAdministeredObjectAnnotations(AnnotationInstance adminObjectDefinitions) {
        if (adminObjectDefinitions == null) {
            return Collections.emptyList();
        }
        AnnotationInstance[] nested = adminObjectDefinitions.value().asNestedArray();
        return Arrays.asList(nested);
    }
}
