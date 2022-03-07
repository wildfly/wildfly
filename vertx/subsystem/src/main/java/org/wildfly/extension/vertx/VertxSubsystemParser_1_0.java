/*
 * Copyright (C) 2020 RedHat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.vertx;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;
import static org.jboss.as.controller.PersistentResourceXMLDescription.decorator;
import static org.wildfly.extension.vertx.VertxConstants.ELEMENT_VERTXES;
import static org.wildfly.extension.vertx.VertxConstants.ELEMENT_VERTX_OPTIONS;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;

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
                        .setXmlWrapperElement(ELEMENT_VERTXES)
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
                          .addChild(
                            builder(EventBusResourceDefinition.INSTANCE.getPathElement())
                              .addAttributes(EventBusResourceDefinition.getVertxEventbusAttrs().toArray(new AttributeDefinition[0]))
                          )
                          .addChild(
                            builder(ClusterNodeMetadataResourceDefinition.INSTANCE.getPathElement())
                              .addAttributes(ClusterNodeMetadataResourceDefinition.getClusterNodeMetaAttrs().toArray(new AttributeDefinition[0]))
                          )
                )
                .build();
    }

    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return xmlDescription;
    }

}
