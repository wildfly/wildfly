/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
class AgroalSubsystemParser_2_0 extends PersistentResourceXMLParser {

    static final AgroalSubsystemParser_2_0 INSTANCE = new AgroalSubsystemParser_2_0();

    private static final PersistentResourceXMLDescription XML_DESCRIPTION;

    static {
        PersistentResourceXMLBuilder subsystemXMLBuilder = builder(AgroalSubsystemDefinition.PATH, AgroalNamespace.AGROAL_2_0.getUriString());

        PersistentResourceXMLBuilder datasourceXMLBuilder = builder(DataSourceDefinition.PATH);
        for (AttributeDefinition attributeDefinition : DataSourceDefinition.ATTRIBUTES) {
            datasourceXMLBuilder.addAttribute(attributeDefinition);
        }
        subsystemXMLBuilder.addChild(datasourceXMLBuilder);

        PersistentResourceXMLBuilder xaDatasourceXMLBuilder = builder(XADataSourceDefinition.PATH);
        for (AttributeDefinition attributeDefinition : XADataSourceDefinition.ATTRIBUTES) {
            xaDatasourceXMLBuilder.addAttribute(attributeDefinition);
        }
        subsystemXMLBuilder.addChild(xaDatasourceXMLBuilder);

        PersistentResourceXMLBuilder driverXMLBuilder = PersistentResourceXMLDescription.builder(DriverDefinition.PATH);
        driverXMLBuilder.setXmlWrapperElement(DriverDefinition.DRIVERS_ELEMENT_NAME);
        for (AttributeDefinition attributeDefinition : DriverDefinition.ATTRIBUTES) {
            driverXMLBuilder.addAttribute(attributeDefinition);
        }
        subsystemXMLBuilder.addChild(driverXMLBuilder);

        XML_DESCRIPTION = subsystemXMLBuilder.build();
    }

    private AgroalSubsystemParser_2_0() {
    }

    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return XML_DESCRIPTION;
    }
}
