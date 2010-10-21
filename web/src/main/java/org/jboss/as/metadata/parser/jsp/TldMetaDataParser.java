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

package org.jboss.as.metadata.parser.jsp;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.metadata.parser.ee.DescriptionGroupMetaDataParser;
import org.jboss.as.metadata.parser.util.MetaDataElementParser;
import org.jboss.metadata.javaee.spec.DescriptionGroupMetaData;
import org.jboss.metadata.web.spec.FunctionMetaData;
import org.jboss.metadata.web.spec.ListenerMetaData;
import org.jboss.metadata.web.spec.TagMetaData;
import org.jboss.metadata.web.spec.Tld11MetaData;
import org.jboss.metadata.web.spec.Tld12MetaData;
import org.jboss.metadata.web.spec.Tld20MetaData;
import org.jboss.metadata.web.spec.Tld21MetaData;
import org.jboss.metadata.web.spec.TldExtensionMetaData;
import org.jboss.metadata.web.spec.TldMetaData;


/**
 * @author Remy Maucherat
 */
public class TldMetaDataParser extends MetaDataElementParser {

    public static TldMetaData parse(XMLStreamReader reader) throws XMLStreamException {

        reader.require(START_DOCUMENT, null, null);
        // Read until the first start element
        Version version = null;
        while (reader.hasNext() && reader.next() != START_ELEMENT) {
            if (reader.getEventType() == DTD) {
                String dtdLocation = readDTDLocation(reader);
                if (dtdLocation != null) {
                    version = Location.getVersion(dtdLocation);
                }
                if (version == null) {
                    // DTD->getText() is incomplete and not parsable with Xerces from Sun JDK 6,
                    // so assume TLD 1.2
                    version = Version.TLD_1_2;
                }
            }
        }
        String schemaLocation = readSchemaLocation(reader);
        if (schemaLocation != null) {
            version = Location.getVersion(schemaLocation);
        }
        if (version == null) {
            // Look at the version attribute
            String versionString = null;
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i ++) {
                if (reader.getAttributeNamespace(i) != null) {
                    continue;
                }
                final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                if (attribute == Attribute.VERSION) {
                    versionString = reader.getAttributeValue(i);
                }
            }
            if ("2.0".equals(versionString)) {
                version = Version.TLD_2_0;
            } else if ("2.1".equals(versionString)) {
                version = Version.TLD_2_1;
            }
        }
        if (version == null) {
            // It is likely an error to not have a version at this point though
            version = Version.TLD_2_1;
        }
        TldMetaData tld = null;
        switch (version) {
            case TLD_1_1: tld = new Tld11MetaData(); break;
            case TLD_1_2: tld = new Tld12MetaData(); break;
            case TLD_2_0: tld = new Tld20MetaData(); break;
            case TLD_2_1: tld = new Tld21MetaData(); break;
        }

        // Handle attributes
        final int count = reader.getAttributeCount();
        for (int i = 0; i < count; i ++) {
            final String value = reader.getAttributeValue(i);
            if (reader.getAttributeNamespace(i) != null) {
                continue;
            }
            final Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case ID: {
                    tld.setId(value);
                    break;
                }
                case VERSION: {
                    tld.setVersion(value);
                    break;
                }
                default: throw unexpectedAttribute(reader, i);
            }
        }

        DescriptionGroupMetaData descriptionGroup = new DescriptionGroupMetaData();
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if (DescriptionGroupMetaDataParser.parse(reader, descriptionGroup)) {
                if (tld.getDescriptionGroup() == null) {
                    tld.setDescriptionGroup(descriptionGroup);
                }
                continue;
            }
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case TLIB_VERSION:
                    tld.setTlibVersion(reader.getElementText());
                    break;
                case TLIBVERSION:
                    if (version == Version.TLD_1_1) {
                        tld.setTlibVersion(reader.getElementText());
                    } else {
                        throw unexpectedElement(reader);
                    }
                    break;
                case SHORT_NAME:
                    tld.setShortName(reader.getElementText());
                    break;
                case SHORTNAME:
                    if (version == Version.TLD_1_1) {
                        tld.setShortName(reader.getElementText());
                    } else {
                        throw unexpectedElement(reader);
                    }
                    break;
                case JSPVERSION:
                    if (version == Version.TLD_1_1) {
                        tld.setJspVersion(reader.getElementText());
                    } else {
                        throw unexpectedElement(reader);
                    }
                    break;
                case JSP_VERSION:
                    if (version == Version.TLD_1_2) {
                        tld.setJspVersion(reader.getElementText());
                    } else if (version != Version.TLD_1_1) {
                        // This is not valid, but still being used in 2.0+ TLDs
                        tld.setJspVersion(reader.getElementText());
                    } else {
                        throw unexpectedElement(reader);
                    }
                    break;
                case INFO:
                    if (version == Version.TLD_1_1) {
                        ((Tld11MetaData) tld).setInfo(reader.getElementText());
                    } else {
                        throw unexpectedElement(reader);
                    }
                    break;
                case URI:
                    tld.setUri(reader.getElementText());
                    break;
                case VALIDATOR:
                    tld.setValidator(ValidatorMetaDataParser.parse(reader));
                    break;
                case LISTENER:
                    List<ListenerMetaData> listeners = tld.getListeners();
                    if (listeners == null) {
                        listeners = new ArrayList<ListenerMetaData>();
                        tld.setListeners(listeners);
                    }
                    listeners.add(ListenerMetaDataParser.parse(reader));
                    break;
                case TAG:
                    List<TagMetaData> tags = tld.getTags();
                    if (tags == null) {
                        tags = new ArrayList<TagMetaData>();
                        tld.setTags(tags);
                    }
                    tags.add(TagMetaDataParser.parse(reader, version));
                    break;
                case FUNCTION:
                    List<FunctionMetaData> functions = tld.getFunctions();
                    if (functions == null) {
                        functions = new ArrayList<FunctionMetaData>();
                        tld.setFunctions(functions);
                    }
                    functions.add(FunctionMetaDataParser.parse(reader));
                    break;
                case TAGLIB_EXTENSION:
                    List<TldExtensionMetaData> extensions = tld.getTaglibExtensions();
                    if (extensions == null) {
                        extensions = new ArrayList<TldExtensionMetaData>();
                        tld.setTaglibExtensions(extensions);
                    }
                    extensions.add(TldExtensionMetaDataParser.parse(reader));
                    break;
               default: throw unexpectedElement(reader);
            }
        }

        return tld;
    }

}
