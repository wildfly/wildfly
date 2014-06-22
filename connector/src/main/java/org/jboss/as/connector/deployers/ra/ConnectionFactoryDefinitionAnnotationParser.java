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

import javax.resource.ConnectionFactoryDefinition;
import javax.resource.ConnectionFactoryDefinitions;
import javax.resource.spi.TransactionSupport;

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
 * Deployment processor responsible for processing {@link ConnectionFactoryDefinition} and {@link ConnectionFactoryDefinitions}
 * and creating {@link org.jboss.as.ee.component.BindingConfiguration}s out of them
 *
 * @author Jesper Pedersen
 */
public class ConnectionFactoryDefinitionAnnotationParser implements DeploymentUnitProcessor {

    private static final DotName CONNECTION_FACTORY_DEFINITION = DotName.createSimple(ConnectionFactoryDefinition.class.getName());
    private static final DotName CONNECTION_FACTORY_DEFINITIONS = DotName.createSimple(ConnectionFactoryDefinitions.class.getName());


    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final CompositeIndex index = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX);
        final EEApplicationClasses applicationClasses = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_CLASSES_DESCRIPTION);

        // @ConnectionFactoryDefinitions
        List<AnnotationInstance> connectionFactoryDefinitions = index.getAnnotations(CONNECTION_FACTORY_DEFINITIONS);
        if (connectionFactoryDefinitions != null) {
            for (AnnotationInstance annotation : connectionFactoryDefinitions) {
                final AnnotationTarget target = annotation.target();
                if (target instanceof ClassInfo == false) {
                    throw EeLogger.ROOT_LOGGER.classOnlyAnnotation("@ConnectionFactoryDefinitions", target);
                }
                // get the nested @ConnectionFactoryDefinition out of the outer @ConnectionFactoryDefinitions
                List<AnnotationInstance> connectionFactories = this.getNestedConnectionFactoryAnnotations(annotation);
                // process the nested @ConnectionFactoryDefinition
                for (AnnotationInstance connectionFactory : connectionFactories) {
                    // create binding configurations out of it
                    this.processConnectionFactoryDefinition(eeModuleDescription, connectionFactory, (ClassInfo) target, applicationClasses);
                }
            }
        }

        // @ConnectionFactoryDefinition
        List<AnnotationInstance> connectionFactories = index.getAnnotations(CONNECTION_FACTORY_DEFINITION);
        if (connectionFactories != null) {
            for (AnnotationInstance connectionFactory : connectionFactories) {
                final AnnotationTarget target = connectionFactory.target();
                if (target instanceof ClassInfo == false) {
                    throw EeLogger.ROOT_LOGGER.classOnlyAnnotation("@ConnectionFactoryDefinition", target);
                }
                // create binding configurations out of it
                this.processConnectionFactoryDefinition(eeModuleDescription, connectionFactory, (ClassInfo) target, applicationClasses);
            }
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private void processConnectionFactoryDefinition(final EEModuleDescription eeModuleDescription, final AnnotationInstance connectionFactoryDefinition, final ClassInfo targetClass, final EEApplicationClasses applicationClasses) throws DeploymentUnitProcessingException {
        // create BindingConfiguration out of the @ConnectionFactoryDefinition annotation
        final BindingConfiguration bindingConfiguration = this.getBindingConfiguration(connectionFactoryDefinition);
        EEModuleClassDescription classDescription = eeModuleDescription.addOrGetLocalClassDescription(targetClass.name().toString());
        // add the binding configuration via a class configurator
        classDescription.getBindingConfigurations().add(bindingConfiguration);
    }

    private BindingConfiguration getBindingConfiguration(final AnnotationInstance connectionFactoryAnnotation) {

        String name = asString(connectionFactoryAnnotation, "name");
        if (name == null || name.isEmpty()) {
            throw EeLogger.ROOT_LOGGER.annotationAttributeMissing("@ConnectionFactoryDefinition", "name");
        }

        // If the name doesn't have a namespace then it defaults to java:comp/env
        if (!name.startsWith("java:")) {
            name = "java:comp/env/" + name;
        }

        String interfaceClz = asString(connectionFactoryAnnotation, "interfaceName");
        if (interfaceClz == null || interfaceClz.equals(Object.class.getName())) {
            throw EeLogger.ROOT_LOGGER.annotationAttributeMissing("@ConnectionFactoryDefinition", "interfaceValue");
        }

        String ra = asString(connectionFactoryAnnotation, "resourceAdapter");
        if (ra == null || ra.equals(Object.class.getName())) {
            throw EeLogger.ROOT_LOGGER.annotationAttributeMissing("@ConnectionFactoryDefinition", "resourceAdapter");
        }

        final DirectConnectionFactoryInjectionSource directConnectionFactoryInjectionSource =
           new DirectConnectionFactoryInjectionSource(name, interfaceClz, ra);

        directConnectionFactoryInjectionSource.setDescription(asString(connectionFactoryAnnotation,
                                                                       DirectConnectionFactoryInjectionSource.DESCRIPTION));
        directConnectionFactoryInjectionSource.setMaxPoolSize(asInt(connectionFactoryAnnotation,
                                                                    DirectConnectionFactoryInjectionSource.MAX_POOL_SIZE));
        directConnectionFactoryInjectionSource.setMinPoolSize(asInt(connectionFactoryAnnotation,
                                                                    DirectConnectionFactoryInjectionSource.MIN_POOL_SIZE));
        directConnectionFactoryInjectionSource.setProperties(asArray(connectionFactoryAnnotation,
                                                                     DirectConnectionFactoryInjectionSource.PROPERTIES));
        directConnectionFactoryInjectionSource.setTransactionSupportLevel(asTransactionSupportLocal(connectionFactoryAnnotation,
                                                                                                    DirectConnectionFactoryInjectionSource.TRANSACTION_SUPPORT));

        final BindingConfiguration bindingDescription = new BindingConfiguration(name, directConnectionFactoryInjectionSource);
        return bindingDescription;
    }

    private int asInt(AnnotationInstance annotation, String string) {
        AnnotationValue value = annotation.value(string);
        return value == null ? -1 : value.asInt();
    }

    private String asString(final AnnotationInstance annotation, String property) {
        AnnotationValue value = annotation.value(property);
        return value == null ? null : value.asString();
    }

    private String[] asArray(final AnnotationInstance annotation, String property) {
        AnnotationValue value = annotation.value(property);
        return value == null ? null : value.asStringArray();
    }

    private TransactionSupport.TransactionSupportLevel asTransactionSupportLocal(final AnnotationInstance annotation, String property) {
        AnnotationValue value = annotation.value(property);
        return value == null ? null : TransactionSupport.TransactionSupportLevel.valueOf((String)value.value());
    }

    /**
     * Returns the nested {@link ConnectionFactoryDefinition} annotations out of the outer {@link ConnectionFactoryDefinitions} annotation
     *
     * @param connectionFactoryDefinitions The outer {@link ConnectionFactoryDefinitions} annotation
     * @return
     */
    private List<AnnotationInstance> getNestedConnectionFactoryAnnotations(AnnotationInstance connectionFactoryDefinitions) {
        if (connectionFactoryDefinitions == null) {
            return Collections.emptyList();
        }
        AnnotationInstance[] nested = connectionFactoryDefinitions.value().asNestedArray();
        return Arrays.asList(nested);
    }
}
