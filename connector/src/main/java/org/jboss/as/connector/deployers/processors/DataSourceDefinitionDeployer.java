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

package org.jboss.as.connector.deployers.processors;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.BindingConfiguration;
import org.jboss.as.ee.component.ClassConfigurator;
import org.jboss.as.ee.component.EEModuleClassConfiguration;
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

import javax.annotation.sql.DataSourceDefinition;
import javax.annotation.sql.DataSourceDefinitions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jboss.as.connector.deployers.processors.DirectDataSourceInjectionSource.DATABASE_NAME_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceInjectionSource.DESCRIPTION_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceInjectionSource.INITIAL_POOL_SIZE_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceInjectionSource.ISOLATION_LEVEL_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceInjectionSource.LOGIN_TIMEOUT_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceInjectionSource.MAX_IDLE_TIME_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceInjectionSource.MAX_POOL_SIZE_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceInjectionSource.MAX_STATEMENTS_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceInjectionSource.MIN_POOL_SIZE_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceInjectionSource.PASSWORD_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceInjectionSource.PORT_NUMBER_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceInjectionSource.PROPERTIES_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceInjectionSource.SERVER_NAME_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceInjectionSource.TRANSACTIONAL_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceInjectionSource.URL_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceInjectionSource.USER_PROP;

/**
 * Deployment processor responsible for processing {@link DataSourceDefinition} and {@link DataSourceDefinitions}
 * and creating {@link BindingConfiguration}s out of them
 *
 * @author John Bailey
 * @author Jason T. Greene
 */
public class DataSourceDefinitionDeployer implements DeploymentUnitProcessor {

    private static final DotName DATASOURCE_DEFINITION = DotName.createSimple(DataSourceDefinition.class.getName());
    private static final DotName DATASOURCE_DEFINITIONS = DotName.createSimple(DataSourceDefinitions.class.getName());

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final CompositeIndex index = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.COMPOSITE_ANNOTATION_INDEX);
        // @DataSourceDefinitions
        List<AnnotationInstance> datasourceDefinitions = index.getAnnotations(DATASOURCE_DEFINITIONS);
        // get the nested @DataSourceDefinition out of the outer @DataSourceDefinitions
        List<AnnotationInstance> nestedDataSources = this.getNestedDataSourceAnnotations(datasourceDefinitions);
        // create binding configurations out of it
        this.processDataSourceDefinitions(eeModuleDescription, nestedDataSources);

        // @DataSourceDefinition
        List<AnnotationInstance> datasources = index.getAnnotations(DATASOURCE_DEFINITION);
        // create binding configurations out of it
        this.processDataSourceDefinitions(eeModuleDescription, datasources);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private void processDataSourceDefinitions(final EEModuleDescription eeModuleDescription, final List<AnnotationInstance> datasourceDefinitions) throws DeploymentUnitProcessingException {
        for (AnnotationInstance annotation : datasourceDefinitions) {
            final AnnotationTarget annotationTarget = annotation.target();
            if (annotationTarget instanceof ClassInfo == false) {
                throw new DeploymentUnitProcessingException("@DataSourceDefinition can only be applied " +
                        "on class. " + annotationTarget + " is not a class");
            }
            // create BindingConfiguration out of the @DataSource annotation
            final BindingConfiguration bindingConfiguration = this.getBindingConfiguration(annotation);
            ClassInfo targetClass = (ClassInfo) annotationTarget;
            EEModuleClassDescription classDescription = eeModuleDescription.getOrAddClassByName(targetClass.name().toString());
            // add the binding configuration via a class configurator
            classDescription.getConfigurators().add(new ClassConfigurator() {
                @Override
                public void configure(DeploymentPhaseContext context, EEModuleClassDescription description, EEModuleClassConfiguration configuration) throws DeploymentUnitProcessingException {
                    configuration.getBindingConfigurations().add(bindingConfiguration);
                }
            });
        }
    }

    private BindingConfiguration getBindingConfiguration(final AnnotationInstance datasourceAnnotation) {

        final AnnotationValue nameValue = datasourceAnnotation.value("name");
        if (nameValue == null || nameValue.asString().isEmpty()) {
            throw new IllegalArgumentException("@DataSourceDefinition annotations must provide a name.");
        }
        String name = nameValue.asString();
        // if the name doesn't have a namespace then it defaults to java:comp/env
        if (!name.startsWith("java:")) {
            name = "java:comp/env/" + name;
        }

        final AnnotationValue classValue = datasourceAnnotation.value("className");
        if (classValue == null || classValue.asString().equals(Object.class.getName())) {
            throw new IllegalArgumentException("@DataSourceDefinition annotations must provide a driver class name.");
        }

        final String type = classValue.asString();
        final DirectDataSourceInjectionSource directDataSourceInjectionSource = new DirectDataSourceInjectionSource();
        directDataSourceInjectionSource.setClassName(type);
        directDataSourceInjectionSource.setDatabaseName(asString(datasourceAnnotation, DATABASE_NAME_PROP));
        directDataSourceInjectionSource.setDescription(asString(datasourceAnnotation, DESCRIPTION_PROP));
        directDataSourceInjectionSource.setInitialPoolSize(asInt(datasourceAnnotation, INITIAL_POOL_SIZE_PROP));
        directDataSourceInjectionSource.setIsolationLevel(asInt(datasourceAnnotation, ISOLATION_LEVEL_PROP));
        directDataSourceInjectionSource.setLoginTimeout(asInt(datasourceAnnotation, LOGIN_TIMEOUT_PROP));
        directDataSourceInjectionSource.setMaxIdleTime(asInt(datasourceAnnotation, MAX_IDLE_TIME_PROP));
        directDataSourceInjectionSource.setMaxStatements(asInt(datasourceAnnotation, MAX_STATEMENTS_PROP));
        directDataSourceInjectionSource.setMaxPoolSize(asInt(datasourceAnnotation, MAX_POOL_SIZE_PROP));
        directDataSourceInjectionSource.setMinPoolSize(asInt(datasourceAnnotation, MIN_POOL_SIZE_PROP));
        directDataSourceInjectionSource.setPassword(asString(datasourceAnnotation, PASSWORD_PROP));
        directDataSourceInjectionSource.setPortNumber(asInt(datasourceAnnotation, PORT_NUMBER_PROP));
        directDataSourceInjectionSource.setProperties(asArray(datasourceAnnotation, PROPERTIES_PROP));
        directDataSourceInjectionSource.setServerName(asString(datasourceAnnotation, SERVER_NAME_PROP));
        directDataSourceInjectionSource.setTransactional(asBool(datasourceAnnotation, TRANSACTIONAL_PROP));
        directDataSourceInjectionSource.setUrl(asString(datasourceAnnotation, URL_PROP));
        directDataSourceInjectionSource.setUser(asString(datasourceAnnotation, USER_PROP));


        final BindingConfiguration bindingDescription = new BindingConfiguration(name, directDataSourceInjectionSource);
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

    private boolean asBool(final AnnotationInstance annotation, String property) {
        AnnotationValue value = annotation.value(property);
        return value == null ? false : value.asBoolean();
    }

    private String[] asArray(final AnnotationInstance annotation, String property) {
        AnnotationValue value = annotation.value(property);
        return value == null ? null : value.asStringArray();
    }

    /**
     * Returns the nested {@link DataSourceDefinition} annotations out of the outer {@link DataSourceDefinitions} annotations
     *
     * @param datasourceDefinitions The outer {@link DataSourceDefinitions} annotations
     * @return
     */
    private List<AnnotationInstance> getNestedDataSourceAnnotations(List<AnnotationInstance> datasourceDefinitions) {
        if (datasourceDefinitions == null || datasourceDefinitions.isEmpty()) {
            return Collections.emptyList();
        }

        List<AnnotationInstance> nestedDataSources = new ArrayList<AnnotationInstance>();
        for (AnnotationInstance datasources : datasourceDefinitions) {
            AnnotationInstance[] nested = datasources.value().asNestedArray();
            nestedDataSources.addAll(Arrays.asList(nested));
        }
        return nestedDataSources;
    }

}
