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

import static org.jboss.as.connector.deployers.processors.DirectDataSourceDescription.DATABASE_NAME_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceDescription.DESCRIPTION_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceDescription.INITIAL_POOL_SIZE_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceDescription.ISOLATION_LEVEL_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceDescription.LOGIN_TIMEOUT_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceDescription.MAX_IDLE_TIME_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceDescription.MAX_POOL_SIZE_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceDescription.MAX_STATEMENTS_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceDescription.MIN_POOL_SIZE_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceDescription.PASSWORD_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceDescription.PORT_NUMBER_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceDescription.PROPERTIES_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceDescription.SERVER_NAME_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceDescription.TRANSACTIONAL_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceDescription.URL_PROP;
import static org.jboss.as.connector.deployers.processors.DirectDataSourceDescription.USER_PROP;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.sql.DataSourceDefinition;
import javax.annotation.sql.DataSourceDefinitions;

import org.jboss.as.ee.component.AbstractComponentConfigProcessor;
import org.jboss.as.ee.component.AbstractComponentDescription;
import org.jboss.as.ee.component.BindingDescription;
import org.jboss.as.ee.component.InterceptorDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

/**
 * Deployment processor responsible for analyzing each attached {@link AbstractComponentDescription} instance to configure
 * required DataSourceDefinition annotations.
 *
 * @author John Bailey
 * @author Jason T. Greene
 */
public class DataSourceDefinitionDeployer extends AbstractComponentConfigProcessor {

    private static final DotName DATASOURCE_DEFINITION = DotName.createSimple(DataSourceDefinition.class.getName());
    private static final DotName DATASOURCE_DEFINITIONS = DotName.createSimple(DataSourceDefinitions.class.getName());

    /** {@inheritDoc} **/
    protected void processComponentConfig(final DeploymentUnit deploymentUnit, final DeploymentPhaseContext phaseContext, final CompositeIndex index, final AbstractComponentDescription description) {
        final ClassInfo classInfo = index.getClassByName(DotName.createSimple(description.getComponentClassName()));
        if(classInfo == null) {
            return; // We can't continue without the annotation index info.
        }
        description.getBindings().addAll(getDatasourceDefinitions(classInfo));

        final Collection<InterceptorDescription> interceptorConfigurations = description.getAllInterceptors().values();
        for (InterceptorDescription interceptorConfiguration : interceptorConfigurations) {
            final ClassInfo interceptorClassInfo = index.getClassByName(DotName.createSimple(interceptorConfiguration.getInterceptorClassName()));
            if(interceptorClassInfo == null) {
                continue;
            }
            interceptorConfiguration.getBindings().addAll(getDatasourceDefinitions(interceptorClassInfo));
        }
    }

    private List<BindingDescription> getDatasourceDefinitions(final ClassInfo classInfo) {
        final List<BindingDescription> configurations = new ArrayList<BindingDescription>();

        final Map<DotName, List<AnnotationInstance>> classAnnotations = classInfo.annotations();
        if (classAnnotations != null) {
            final List<AnnotationInstance> definitions = classAnnotations.get(DATASOURCE_DEFINITION);
            if (definitions != null) for (AnnotationInstance annotation : definitions) {
                if (annotation.target() instanceof ClassInfo) {
                    configurations.add(processDefinition(annotation));
                }
            }
            configurations.addAll(processDefinitions(classAnnotations));
        }

        return configurations;
    }

    private BindingDescription processDefinition(final AnnotationInstance annotation) {

        final AnnotationValue nameValue = annotation.value("name");
        if (nameValue == null || nameValue.asString().isEmpty()) {
            throw new IllegalArgumentException("@DataSourceDefinition annotations must provide a name.");
        }
        final String name = nameValue.asString();

        final AnnotationValue classValue = annotation.value("className");
        if (classValue == null || classValue.asString().equals(Object.class.getName())) {
            throw new IllegalArgumentException("@DataSourceDefinition annotations must provide a driver class name.");
        }

        final String type = classValue.asString();
        final DirectDataSourceDescription desc = new DirectDataSourceDescription();
        desc.setClassName(type);
        desc.setDatabaseName(asString(annotation, DATABASE_NAME_PROP));
        desc.setDescription(asString(annotation, DESCRIPTION_PROP));
        desc.setInitialPoolSize(asInt(annotation, INITIAL_POOL_SIZE_PROP));
        desc.setIsolationLevel(asInt(annotation, ISOLATION_LEVEL_PROP));
        desc.setLoginTimeout(asInt(annotation, LOGIN_TIMEOUT_PROP));
        desc.setMaxIdleTime(asInt(annotation, MAX_IDLE_TIME_PROP));
        desc.setMaxStatements(asInt(annotation, MAX_STATEMENTS_PROP));
        desc.setMaxPoolSize(asInt(annotation, MAX_POOL_SIZE_PROP));
        desc.setMinPoolSize(asInt(annotation, MIN_POOL_SIZE_PROP));
        desc.setPassword(asString(annotation, PASSWORD_PROP));
        desc.setPortNumber(asInt(annotation, PORT_NUMBER_PROP));
        desc.setProperties(asArray(annotation, PROPERTIES_PROP));
        desc.setServerName(asString(annotation, SERVER_NAME_PROP));
        desc.setTransactional(asBool(annotation, TRANSACTIONAL_PROP));
        desc.setUrl(asString(annotation, URL_PROP));
        desc.setUser(asString(annotation, USER_PROP));


        final BindingDescription bindingDescription = new BindingDescription();
        bindingDescription.setDependency(true);
        bindingDescription.setBindingName(name);
        bindingDescription.setBindingType(type);
        bindingDescription.setAbsoluteBinding(true);
        bindingDescription.setReferenceSourceDescription(desc);
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

    private List<BindingDescription> processDefinitions(final Map<DotName, List<AnnotationInstance>> classAnnotations) {
        final List<AnnotationInstance> definitionsAnnotation = classAnnotations.get(DATASOURCE_DEFINITIONS);
        if (definitionsAnnotation == null || definitionsAnnotation.isEmpty()) {
            return Collections.emptyList();
        }

        final AnnotationInstance resourcesInstance = definitionsAnnotation.get(0);
        final AnnotationInstance[] definitions = resourcesInstance.value().asNestedArray();

        final List<BindingDescription> definitionDescriptions = new ArrayList<BindingDescription>(definitions.length);
        for (AnnotationInstance definition : definitions) {
            definitionDescriptions.add(processDefinition(definition));
        }
        return definitionDescriptions;
    }
}
