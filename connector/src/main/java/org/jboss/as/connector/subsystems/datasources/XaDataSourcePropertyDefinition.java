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

import static org.jboss.as.connector.subsystems.datasources.Constants.XADATASOURCE_PROPERTIES;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author Stefano Maestri
 */
public class XaDataSourcePropertyDefinition extends SimpleResourceDefinition {
    protected static final PathElement PATH_PROPERTIES = PathElement.pathElement(XADATASOURCE_PROPERTIES.getName());
    static final XaDataSourcePropertyDefinition INSTANCE = new XaDataSourcePropertyDefinition(false);
    static final XaDataSourcePropertyDefinition DEPLOYED_INSTANCE = new XaDataSourcePropertyDefinition(true);

    private final boolean deployed;

    private XaDataSourcePropertyDefinition(final boolean deployed) {
        super(PATH_PROPERTIES,
                DataSourcesExtension.getResourceDescriptionResolver("xa-data-source", "xa-datasource-properties"),
                deployed ? null : XaDataSourcePropertyAdd.INSTANCE,
                deployed ? null : XaDataSourcePropertyRemove.INSTANCE);
        this.deployed = deployed;

    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        if (deployed) {
            SimpleAttributeDefinition runtimeAttribute = new SimpleAttributeDefinitionBuilder(Constants.XADATASOURCE_PROPERTY_VALUE).setFlags(AttributeAccess.Flag.STORAGE_RUNTIME).build();
            resourceRegistration.registerReadOnlyAttribute(runtimeAttribute, XMLXaDataSourceRuntimeHandler.INSTANCE);
        } else {
            resourceRegistration.registerReadOnlyAttribute(Constants.XADATASOURCE_PROPERTY_VALUE, null);
        }
    }

}
