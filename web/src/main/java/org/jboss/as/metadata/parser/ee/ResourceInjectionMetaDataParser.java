/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.metadata.parser.ee;

import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.metadata.parser.util.MetaDataElementParser;
import org.jboss.metadata.javaee.spec.DescriptionGroupMetaData;
import org.jboss.metadata.javaee.spec.DescriptionsImpl;
import org.jboss.metadata.javaee.spec.DisplayNamesImpl;
import org.jboss.metadata.javaee.spec.EmptyMetaData;
import org.jboss.metadata.javaee.spec.IconsImpl;
import org.jboss.metadata.javaee.spec.ResourceInjectionMetaData;
import org.jboss.metadata.javaee.spec.ResourceInjectionTargetMetaData;
import org.jboss.metadata.javaee.support.ResourceInjectionMetaDataWithDescriptions;


/**
 * @author Remy Maucherat
 */
public class ResourceInjectionMetaDataParser extends MetaDataElementParser {

    public static boolean parse(XMLStreamReader reader, ResourceInjectionMetaData resourceInjection) throws XMLStreamException {
        // Only look at the current element, no iteration
        final Element element = Element.forName(reader.getLocalName());
        switch (element) {
            case LOOKUP_NAME:
                resourceInjection.setLookupName(reader.getElementText());
                break;
            case MAPPED_NAME:
                resourceInjection.setMappedName(reader.getElementText());
                break;
            case JNDI_NAME:
                resourceInjection.setJndiName(reader.getElementText());
                break;
            case IGNORE_DEPENDECY:
                resourceInjection.setIgnoreDependency(new EmptyMetaData());
                break;
            case INJECTION_TARGET:
                Set<ResourceInjectionTargetMetaData> injectionTargets = resourceInjection.getInjectionTargets();
                if (injectionTargets == null) {
                    injectionTargets = new HashSet<ResourceInjectionTargetMetaData>();
                    resourceInjection.setInjectionTargets(injectionTargets);
                }
                injectionTargets.add(ResourceInjectionTargetMetaDataParser.parse(reader));
                break;
            default: return false;
        }
        return true;
    }

}
