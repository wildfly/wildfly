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
import org.jboss.metadata.web.spec.AttributeMetaData;
import org.jboss.metadata.web.spec.BodyContentType;
import org.jboss.metadata.web.spec.Tag11MetaData;
import org.jboss.metadata.web.spec.Tag12MetaData;
import org.jboss.metadata.web.spec.TagMetaData;
import org.jboss.metadata.web.spec.TldExtensionMetaData;
import org.jboss.metadata.web.spec.VariableMetaData;

/**
 * @author Remy Maucherat
 */
public class TagMetaDataParser extends MetaDataElementParser {

    public static TagMetaData parse(XMLStreamReader reader, Version version) throws XMLStreamException {
        TagMetaData tag = null;
        switch (version) {
            case TLD_1_1:
                tag = new Tag11MetaData();
                break;
            case TLD_1_2:
                tag = new Tag12MetaData();
                break;
            default: tag = new TagMetaData();
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
                    tag.setId(value);
                    break;
                }
                default: throw unexpectedAttribute(reader, i);
            }
        }

        DescriptionGroupMetaData descriptionGroup = new DescriptionGroupMetaData();
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if (DescriptionGroupMetaDataParser.parse(reader, descriptionGroup)) {
                if (tag.getDescriptionGroup() == null) {
                    tag.setDescriptionGroup(descriptionGroup);
                }
                continue;
            }
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case NAME:
                    tag.setName(reader.getElementText());
                    break;
                case TAG_CLASS:
                    tag.setTagClass(reader.getElementText());
                    break;
                case TAGCLASS:
                    if (version == Version.TLD_1_1) {
                        tag.setTagClass(reader.getElementText());
                    } else {
                        throw unexpectedElement(reader);
                    }
                    break;
                case TEI_CLASS:
                    tag.setTeiClass(reader.getElementText());
                    break;
                case TEICLASS:
                    if (version == Version.TLD_1_1) {
                        tag.setTeiClass(reader.getElementText());
                    } else {
                        throw unexpectedElement(reader);
                    }
                    break;
                case BODY_CONTENT:
                    tag.setBodyContent(BodyContentType.valueOf(reader.getElementText()));
                    break;
                case BODYCONTENT:
                    if (version == Version.TLD_1_1) {
                        tag.setBodyContent(BodyContentType.valueOf(reader.getElementText()));
                    } else {
                        throw unexpectedElement(reader);
                    }
                    break;
                case INFO:
                    if (version == Version.TLD_1_1) {
                        ((Tag11MetaData) tag).setInfo(reader.getElementText());
                    } else {
                        throw unexpectedElement(reader);
                    }
                    break;
                case SMALL_ICON:
                    if (version == Version.TLD_1_2) {
                        ((Tag12MetaData) tag).setSmallIcon(reader.getElementText());
                    } else {
                        throw unexpectedElement(reader);
                    }
                    break;
                case LARGE_ICON:
                    if (version == Version.TLD_1_2) {
                        ((Tag12MetaData) tag).setLargeIcon(reader.getElementText());
                    } else {
                        throw unexpectedElement(reader);
                    }
                    break;
                case VARIABLE:
                    List<VariableMetaData> variables = tag.getVariables();
                    if (variables == null) {
                        variables = new ArrayList<VariableMetaData>();
                        tag.setVariables(variables);
                    }
                    variables.add(VariableMetaDataParser.parse(reader));
                    break;
                case ATTRIBUTE:
                    List<AttributeMetaData> attributes = tag.getAttributes();
                    if (attributes == null) {
                        attributes = new ArrayList<AttributeMetaData>();
                        tag.setAttributes(attributes);
                    }
                    attributes.add(AttributeMetaDataParser.parse(reader));
                    break;
                case DYNAMIC_ATTRIBUTES:
                    tag.setDynamicAttributes(reader.getElementText());
                    break;
                case EXAMPLE:
                    List<String> examples = tag.getExamples();
                    if (examples == null) {
                        examples = new ArrayList<String>();
                        tag.setExamples(examples);
                    }
                    examples.add(reader.getElementText());
                    break;
                case TAG_EXTENSION:
                    List<TldExtensionMetaData> extensions = tag.getTagExtensions();
                    if (extensions == null) {
                        extensions = new ArrayList<TldExtensionMetaData>();
                        tag.setTagExtensions(extensions);
                    }
                    extensions.add(TldExtensionMetaDataParser.parse(reader));
                    break;
                default: throw unexpectedElement(reader);
            }
        }

        return tag;
    }

}
