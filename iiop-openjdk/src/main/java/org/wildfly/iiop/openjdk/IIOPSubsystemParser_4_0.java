/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.iiop.openjdk;


import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;

/**
 * <p>
 * This class implements a parser for the IIOP subsystem version 4.0.
 * </p>
 */
public class IIOPSubsystemParser_4_0 extends PersistentResourceXMLParser {


    IIOPSubsystemParser_4_0() {
    }

    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return builder(IIOPExtension.PATH_SUBSYSTEM, Namespace.IIOP_OPENJDK_4_0.getUriString())
                .setMarshallDefaultValues(true)
                .addAttributes(IIOPRootDefinition.ALL_ATTRIBUTES.toArray(new AttributeDefinition[0]))
                .build();
    }

}
