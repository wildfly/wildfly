/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.vertx;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;
import static org.jboss.as.controller.PersistentResourceXMLDescription.decorator;
import static org.wildfly.extension.vertx.VertxConstants.ELEMENT_VERTX_OPTIONS;

/**
 * Parser used to parse the Vertx Subsystem.
 *
 * @author <a href="aoingl@gmail.com">Lin Gao</a>
 */
public class VertxSubsystemParser_1_0 extends PersistentResourceXMLParser {
    /**
     * The name space used for the {@code subsystem} element
     */
    static final String NAMESPACE = "urn:wildfly:vertx:1.0";

    private static final PersistentResourceXMLDescription xmlDescription;
    static {
        xmlDescription = builder(VertxSubsystemExtension.SUBSYSTEM_PATH, NAMESPACE)
          .addChild(
            builder(VertxResourceDefinition.INSTANCE.getPathElement())
              .addAttributes(VertxAttributes.getSimpleAttributes().toArray(new AttributeDefinition[0]))
          )
          .addChild(
            decorator(ELEMENT_VERTX_OPTIONS)
              .addChild(
                builder(VertxOptionFileResourceDefinition.INSTANCE.getPathElement())
                  .addAttributes(VertxOptionsAttributes.getVertxOptionsFileAttributes().toArray(new AttributeDefinition[0]))
              )
              .addChild(
                builder(VertxOptionsResourceDefinition.INSTANCE.getPathElement())
                  .addAttributes(VertxOptionsAttributes.getVertxOptionsAttributes().toArray(new AttributeDefinition[0]))
              )
              .addChild(
                builder(AddressResolverResourceDefinition.INSTANCE.getPathElement())
                  .addAttributes(AddressResolverResourceDefinition.getVertxAddressResolverOptionsAttrs().toArray(new AttributeDefinition[0]))
              )
          )
          .build();
    }

    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return xmlDescription;
    }

}
