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

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

/**
 * The subsystem parser, which uses stax to read and write to and from xml
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
                .addAttributes(VertxAttributes.EVENT_LOOP_POOL_SIZE,
                        VertxAttributes.HA_ENABLED)
                .build();
    }

    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return xmlDescription;
    }

}
