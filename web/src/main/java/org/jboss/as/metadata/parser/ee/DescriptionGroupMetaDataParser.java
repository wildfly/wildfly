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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.metadata.parser.util.MetaDataElementParser;
import org.jboss.metadata.javaee.spec.DescriptionGroupMetaData;
import org.jboss.metadata.javaee.spec.DescriptionsImpl;
import org.jboss.metadata.javaee.spec.DisplayNamesImpl;
import org.jboss.metadata.javaee.spec.IconsImpl;


/**
 * @author Remy Maucherat
 */
public class DescriptionGroupMetaDataParser extends MetaDataElementParser {

    public static boolean parse(XMLStreamReader reader, DescriptionGroupMetaData descriptionGroup) throws XMLStreamException {
        // Only look at the current element, no iteration
        final Element element = Element.forName(reader.getLocalName());
        switch (element) {
            case DESCRIPTION:
                DescriptionsImpl descriptions = (DescriptionsImpl) descriptionGroup.getDescriptions();
                if (descriptions == null) {
                    descriptions = new DescriptionsImpl();
                    descriptionGroup.setDescriptions(descriptions);
                }
                descriptions.add(DescriptionMetaDataParser.parse(reader));
                break;
            case ICON:
                IconsImpl icons = (IconsImpl) descriptionGroup.getIcons();
                if (icons == null) {
                    icons = new IconsImpl();
                    descriptionGroup.setIcons(icons);
                }
                icons.add(IconMetaDataParser.parse(reader));
                break;
            case DISPLAY_NAME:
                DisplayNamesImpl displayNames = (DisplayNamesImpl) descriptionGroup.getDisplayNames();
                if (displayNames == null) {
                    displayNames = new DisplayNamesImpl();
                    descriptionGroup.setDisplayNames(displayNames);
                }
                displayNames.add(DisplayNameMetaDataParser.parse(reader));
                break;
            default: return false;
        }
        return true;
    }

}
