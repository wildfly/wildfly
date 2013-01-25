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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.DiscardUndefinedAttributesTransformer;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

import static org.jboss.as.connector.subsystems.datasources.Constants.JDBC_DRIVER_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTER_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

/**
 * Stefano Maestri
 */
public class JdbcDriverDefinition extends SimpleResourceDefinition {
    protected static final PathElement PATH_DRIVER = PathElement.pathElement(JDBC_DRIVER_NAME);
    static final JdbcDriverDefinition INSTANCE = new JdbcDriverDefinition();

    private JdbcDriverDefinition() {
        super(PATH_DRIVER,
                DataSourcesExtension.getResourceDescriptionResolver(JDBC_DRIVER_NAME),
                JdbcDriverAdd.INSTANCE,
                JdbcDriverRemove.INSTANCE);

    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attribute : Constants.JDBC_DRIVER_ATTRIBUTES) {
            resourceRegistration.registerReadOnlyAttribute(attribute, null);
        }
    }

    static void registerTransformers110(ResourceTransformationDescriptionBuilder parenBuilder) {

        parenBuilder.addChildResource(PATH_DRIVER).getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, Constants.DRIVER_MINOR_VERSION, Constants.DRIVER_MAJOR_VERSION)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, Constants.DRIVER_DATASOURCE_CLASS_NAME)
                .end();
    }

}
