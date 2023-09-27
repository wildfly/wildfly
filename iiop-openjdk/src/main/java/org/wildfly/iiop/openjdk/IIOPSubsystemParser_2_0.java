/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.iiop.openjdk;


import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;
import org.jboss.dmr.ModelNode;

/**
 * <p>
 * This class implements a parser for the IIOP subsystem.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class IIOPSubsystemParser_2_0 extends PersistentResourceXMLParser {


    IIOPSubsystemParser_2_0() {
    }

    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return builder(IIOPExtension.PATH_SUBSYSTEM, Namespace.IIOP_OPENJDK_2_0.getUriString())
                .setMarshallDefaultValues(true)
                .addAttributes(IIOPRootDefinition.ALL_ATTRIBUTES.toArray(new AttributeDefinition[0]))
                .setAdditionalOperationsGenerator((address, addOperation, operations) -> {
                    if(!addOperation.get(IIOPRootDefinition.SOCKET_BINDING.getName()).isDefined()){
                        addOperation.get(IIOPRootDefinition.SOCKET_BINDING.getName()).set(new ModelNode().set("iiop"));
                    }
                })
                .build();
    }

}