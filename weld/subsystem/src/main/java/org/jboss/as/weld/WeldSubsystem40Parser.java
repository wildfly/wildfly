/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.weld;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;

class WeldSubsystem40Parser extends PersistentResourceXMLParser {

    public static final String NAMESPACE = "urn:jboss:domain:weld:4.0";
    static final WeldSubsystem40Parser INSTANCE = new WeldSubsystem40Parser();
    private static final PersistentResourceXMLDescription xmlDescription;

    static {
        xmlDescription = PersistentResourceXMLDescription.builder(WeldResourceDefinition.INSTANCE, NAMESPACE)
                .addAttributes(WeldResourceDefinition.NON_PORTABLE_MODE_ATTRIBUTE, WeldResourceDefinition.REQUIRE_BEAN_DESCRIPTOR_ATTRIBUTE,
                        WeldResourceDefinition.DEVELOPMENT_MODE_ATTRIBUTE, WeldResourceDefinition.THREAD_POOL_SIZE_ATTRIBUTE)
                .build();
    }

    private WeldSubsystem40Parser() {
    }

    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return xmlDescription;
    }
}
