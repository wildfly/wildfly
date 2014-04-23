/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.deployers.datasource;

import org.jboss.as.ee.resource.definition.ResourceDefinitionAnnotationProcessor;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.metadata.property.PropertyReplacer;

import javax.annotation.sql.DataSourceDefinition;
import javax.annotation.sql.DataSourceDefinitions;

/**
 * @author John Bailey
 * @author Jason T. Greene
 * @author Eduardo Martins
 */
public class DataSourceDefinitionAnnotationProcessor extends ResourceDefinitionAnnotationProcessor {

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
        final DataSourceDefinitionInjectionSource directDataSourceInjectionSource = new DataSourceDefinitionInjectionSource(AnnotationElement.asRequiredString(annotationInstance, AnnotationElement.NAME));
        directDataSourceInjectionSource.setClassName(AnnotationElement.asRequiredString(annotationInstance, "className"));
        directDataSourceInjectionSource.setDatabaseName(AnnotationElement.asOptionalString(annotationInstance, DataSourceDefinitionInjectionSource.DATABASE_NAME_PROP));
        directDataSourceInjectionSource.setDescription(AnnotationElement.asOptionalString(annotationInstance, DataSourceDefinitionInjectionSource.DESCRIPTION_PROP));
        directDataSourceInjectionSource.setInitialPoolSize(AnnotationElement.asOptionalInt(annotationInstance, DataSourceDefinitionInjectionSource.INITIAL_POOL_SIZE_PROP));
        directDataSourceInjectionSource.setIsolationLevel(AnnotationElement.asOptionalInt(annotationInstance, DataSourceDefinitionInjectionSource.ISOLATION_LEVEL_PROP));
        directDataSourceInjectionSource.setLoginTimeout(AnnotationElement.asOptionalInt(annotationInstance, DataSourceDefinitionInjectionSource.LOGIN_TIMEOUT_PROP));
        directDataSourceInjectionSource.setMaxIdleTime(AnnotationElement.asOptionalInt(annotationInstance, DataSourceDefinitionInjectionSource.MAX_IDLE_TIME_PROP));
        directDataSourceInjectionSource.setMaxStatements(AnnotationElement.asOptionalInt(annotationInstance, DataSourceDefinitionInjectionSource.MAX_STATEMENTS_PROP));
        directDataSourceInjectionSource.setMaxPoolSize(AnnotationElement.asOptionalInt(annotationInstance, DataSourceDefinitionInjectionSource.MAX_POOL_SIZE_PROP));
        directDataSourceInjectionSource.setMinPoolSize(AnnotationElement.asOptionalInt(annotationInstance, DataSourceDefinitionInjectionSource.MIN_POOL_SIZE_PROP));
        directDataSourceInjectionSource.setInitialPoolSize(AnnotationElement.asOptionalInt(annotationInstance, DataSourceDefinitionInjectionSource.INITIAL_POOL_SIZE_PROP));
        directDataSourceInjectionSource.setPassword(AnnotationElement.asOptionalString(annotationInstance, DataSourceDefinitionInjectionSource.PASSWORD_PROP));
        directDataSourceInjectionSource.setPortNumber(AnnotationElement.asOptionalInt(annotationInstance, DataSourceDefinitionInjectionSource.PORT_NUMBER_PROP));
        directDataSourceInjectionSource.addProperties(AnnotationElement.asOptionalStringArray(annotationInstance, AnnotationElement.PROPERTIES));
        directDataSourceInjectionSource.setServerName(AnnotationElement.asOptionalString(annotationInstance, DataSourceDefinitionInjectionSource.SERVER_NAME_PROP));
        directDataSourceInjectionSource.setTransactional(AnnotationElement.asOptionalBoolean(annotationInstance, DataSourceDefinitionInjectionSource.TRANSACTIONAL_PROP));
        directDataSourceInjectionSource.setUrl(AnnotationElement.asOptionalString(annotationInstance, DataSourceDefinitionInjectionSource.URL_PROP));
        directDataSourceInjectionSource.setUser(AnnotationElement.asOptionalString(annotationInstance, DataSourceDefinitionInjectionSource.USER_PROP));
        return directDataSourceInjectionSource;
    }

}
