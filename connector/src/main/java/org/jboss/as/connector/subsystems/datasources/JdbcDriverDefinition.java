/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2012, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.jboss.as.connector.subsystems.datasources;

import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_CLASS_INFO;
import static org.jboss.as.connector.subsystems.datasources.Constants.JDBC_DRIVER_NAME;

import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Stefano Maestri
 */
public class JdbcDriverDefinition extends SimpleResourceDefinition {
    protected static final PathElement PATH_DRIVER = PathElement.pathElement(JDBC_DRIVER_NAME);

    private final boolean runtimeOnly;

    private final List<AccessConstraintDefinition> accessConstraints;

    static JdbcDriverDefinition createInstance(final boolean runtimeOnly) {
        return new JdbcDriverDefinition(runtimeOnly);
    }

    private JdbcDriverDefinition(final boolean runtimeOnly) {
        super(PATH_DRIVER,
                DataSourcesExtension.getResourceDescriptionResolver(JDBC_DRIVER_NAME),
                JdbcDriverAdd.INSTANCE,
                JdbcDriverRemove.INSTANCE);
        this.runtimeOnly = runtimeOnly;
        ApplicationTypeConfig atc = new ApplicationTypeConfig(DataSourcesExtension.SUBSYSTEM_NAME, JDBC_DRIVER_NAME);
        accessConstraints = new ApplicationTypeAccessConstraintDefinition(atc).wrapAsList();
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attribute : Constants.JDBC_DRIVER_ATTRIBUTES) {
            resourceRegistration.registerReadOnlyAttribute(attribute, null);
        }
        if (runtimeOnly) {
            resourceRegistration.registerMetric(DATASOURCE_CLASS_INFO, GetDataSourceClassInfoOperationHandler.INSTANCE);
        }
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }

    static void registerTransformers110(ResourceTransformationDescriptionBuilder parenBuilder) {

        parenBuilder.addChildResource(PATH_DRIVER).getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, Constants.DRIVER_MINOR_VERSION, Constants.DRIVER_MAJOR_VERSION)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, Constants.DRIVER_DATASOURCE_CLASS_NAME)
                .end();
    }

    static void registerTransformers111(ResourceTransformationDescriptionBuilder parenBuilder) {

        parenBuilder.addChildResource(PATH_DRIVER).getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.UNDEFINED, Constants.DRIVER_DATASOURCE_CLASS_NAME)
                .end();
    }

}
