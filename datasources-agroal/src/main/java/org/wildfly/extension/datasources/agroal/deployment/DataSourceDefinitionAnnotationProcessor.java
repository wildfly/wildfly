/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.datasources.agroal.deployment;

import org.jboss.as.ee.resource.definition.ResourceDefinitionAnnotationProcessor;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.metadata.property.PropertyReplacer;

import javax.annotation.sql.DataSourceDefinition;
import javax.annotation.sql.DataSourceDefinitions;

import static org.jboss.as.ee.resource.definition.ResourceDefinitionAnnotationProcessor.AnnotationElement.*;

/**
 * Processor of the DataSourceDefinition annotation
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class DataSourceDefinitionAnnotationProcessor extends ResourceDefinitionAnnotationProcessor {

    private static final String NAME_PROP = "name";
    private static final String CLASS_NAME_PROP = "className";
    private static final String DESCRIPTION_PROP = "description";
    private static final String URL_PROP = "url";
    private static final String USER_PROP = "user";
    private static final String PASSWORD_PROP = "password";
    private static final String DATABASE_NAME_PROP = "databaseName";
    private static final String PORT_NUMBER_PROP = "portNumber";
    private static final String SERVER_NAME_PROP = "serverName";
    private static final String ISOLATION_LEVEL_PROP = "isolationLevel";
    private static final String TRANSACTIONAL_PROP = "transactional";
    private static final String INITIAL_POOL_SIZE_PROP = "initialPoolSize";
    private static final String MAX_POOL_SIZE_PROP = "maxPoolSize";
    private static final String MIN_POOL_SIZE_PROP = "minPoolSize";
    private static final String MAX_IDLE_TIME_PROP = "maxIdleTime";
    private static final String MAX_STATEMENTS_PROP = "maxStatements";
    private static final String PROPERTIES_PROP = "properties";
    private static final String LOGIN_TIMEOUT_PROP = "loginTimeout";

    private static final DotName DATASOURCE_DEFINITION = DotName.createSimple(DataSourceDefinition.class.getName());
    private static final DotName DATASOURCE_DEFINITIONS = DotName.createSimple(DataSourceDefinitions.class.getName());

    @Override
    protected DotName getAnnotationDotName() {
        return DATASOURCE_DEFINITION;
    }

    @Override
    protected DotName getAnnotationCollectionDotName() {
        return DATASOURCE_DEFINITIONS;
    }

    @Override
    protected ResourceDefinitionInjectionSource processAnnotation(AnnotationInstance annotationInstance, PropertyReplacer propertyReplacer) throws DeploymentUnitProcessingException {
        DataSourceDefinitionInjectionSource injectionSource = new DataSourceDefinitionInjectionSource(asRequiredString(annotationInstance, NAME_PROP));

        injectionSource.setClassName(propertyReplacer.replaceProperties(asRequiredString(annotationInstance, CLASS_NAME_PROP)));
        injectionSource.setDatabaseName(propertyReplacer.replaceProperties(asOptionalString(annotationInstance, DATABASE_NAME_PROP)));
        injectionSource.setDescription(propertyReplacer.replaceProperties(asOptionalString(annotationInstance, DESCRIPTION_PROP)));
        injectionSource.setInitialPoolSize(asOptionalInt(annotationInstance, INITIAL_POOL_SIZE_PROP));
        injectionSource.setIsolationLevel(asOptionalInt(annotationInstance, ISOLATION_LEVEL_PROP));
        injectionSource.setLoginTimeout(asOptionalInt(annotationInstance, LOGIN_TIMEOUT_PROP));
        injectionSource.setMaxIdleTime(asOptionalInt(annotationInstance, MAX_IDLE_TIME_PROP));
        injectionSource.setMaxStatements(asOptionalInt(annotationInstance, MAX_STATEMENTS_PROP));
        injectionSource.setMaxPoolSize(asOptionalInt(annotationInstance, MAX_POOL_SIZE_PROP));
        injectionSource.setMinPoolSize(asOptionalInt(annotationInstance, MIN_POOL_SIZE_PROP));
        injectionSource.setInitialPoolSize(asOptionalInt(annotationInstance, INITIAL_POOL_SIZE_PROP));
        injectionSource.setPassword(propertyReplacer.replaceProperties(asOptionalString(annotationInstance, PASSWORD_PROP)));
        injectionSource.setPortNumber(asOptionalInt(annotationInstance, PORT_NUMBER_PROP));
        injectionSource.addProperties(asOptionalStringArray(annotationInstance, PROPERTIES_PROP));
        injectionSource.setServerName(propertyReplacer.replaceProperties(asOptionalString(annotationInstance, SERVER_NAME_PROP)));
        injectionSource.setTransactional(asOptionalBoolean(annotationInstance, TRANSACTIONAL_PROP));
        injectionSource.setUrl(propertyReplacer.replaceProperties(asOptionalString(annotationInstance, URL_PROP)));
        injectionSource.setUser(propertyReplacer.replaceProperties(asOptionalString(annotationInstance, USER_PROP)));

        return injectionSource;
    }
}
