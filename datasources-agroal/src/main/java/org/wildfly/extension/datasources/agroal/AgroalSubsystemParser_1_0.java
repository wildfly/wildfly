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
package org.wildfly.extension.datasources.agroal;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLDescription.PersistentResourceXMLBuilder;
import org.jboss.as.controller.PersistentResourceXMLParser;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

/**
 * The subsystem parser and marshaller, that reads the model to and from it's xml persistent representation
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
class AgroalSubsystemParser_1_0 extends PersistentResourceXMLParser {

    static final AgroalSubsystemParser_1_0 INSTANCE = new AgroalSubsystemParser_1_0();

    private static final PersistentResourceXMLDescription XML_DESCRIPTION;

    static {
        PersistentResourceXMLBuilder subsystemXMLBuilder = builder(AgroalSubsystemDefinition.INSTANCE.getPathElement(), AgroalNamespace.AGROAL_1_0.getUriString());

        PersistentResourceXMLBuilder datasourceXMLBuilder = builder(DataSourceDefinition.INSTANCE.getPathElement());
        for (AttributeDefinition attributeDefinition : DataSourceDefinition.ATTRIBUTES) {
            datasourceXMLBuilder.addAttribute(attributeDefinition);
        }
        subsystemXMLBuilder.addChild(datasourceXMLBuilder);

        PersistentResourceXMLBuilder xaDatasourceXMLBuilder = builder(XADataSourceDefinition.INSTANCE.getPathElement());
        for (AttributeDefinition attributeDefinition : XADataSourceDefinition.ATTRIBUTES) {
            xaDatasourceXMLBuilder.addAttribute(attributeDefinition);
        }
        subsystemXMLBuilder.addChild(xaDatasourceXMLBuilder);

        PersistentResourceXMLBuilder driverXMLBuilder = PersistentResourceXMLDescription.builder(DriverDefinition.INSTANCE.getPathElement());
        driverXMLBuilder.setXmlWrapperElement(DriverDefinition.DRIVERS_ELEMENT_NAME);
        for (AttributeDefinition attributeDefinition : DriverDefinition.ATTRIBUTES) {
            driverXMLBuilder.addAttribute(attributeDefinition);
        }
        subsystemXMLBuilder.addChild(driverXMLBuilder);

        XML_DESCRIPTION = subsystemXMLBuilder.build();
    }

    private AgroalSubsystemParser_1_0() {
    }

    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return XML_DESCRIPTION;
    }
}
