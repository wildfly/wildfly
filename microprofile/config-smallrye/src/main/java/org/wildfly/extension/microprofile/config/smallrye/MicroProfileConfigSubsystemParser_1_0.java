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

package org.wildfly.extension.microprofile.config.smallrye;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
class MicroProfileConfigSubsystemParser_1_0 extends PersistentResourceXMLParser {
    /**
     * The name space used for the {@code subsystem} element
     */
    public static final String NAMESPACE = "urn:wildfly:microprofile-config-smallrye:1.0";

    private static final PersistentResourceXMLDescription xmlDescription;

    static {
        xmlDescription = builder(MicroProfileConfigExtension.SUBSYSTEM_PATH, NAMESPACE)
                .addChild(builder(MicroProfileConfigExtension.CONFIG_SOURCE_PATH)
                        .addAttributes(
                                ConfigSourceDefinition.ORDINAL,
                                ConfigSourceDefinition.PROPERTIES,
                                ConfigSourceDefinition.CLASS,
                                ConfigSourceDefinition.DIR))
                .addChild(builder(MicroProfileConfigExtension.CONFIG_SOURCE_PROVIDER_PATH)
                        .addAttributes(
                                ConfigSourceProviderDefinition.CLASS))
                .build();
    }

    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return xmlDescription;
    }
}
