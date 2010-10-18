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

package org.jboss.as.metadata.parser.servlet;

import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.metadata.parser.ee.DescriptionGroupMetaDataParser;
import org.jboss.as.metadata.parser.ee.EnvironmentRefsGroupMetaDataParser;
import org.jboss.as.metadata.parser.util.MetaDataElementParser;
import org.jboss.metadata.javaee.spec.DescriptionGroupMetaData;
import org.jboss.metadata.javaee.spec.EnvironmentRefsGroupMetaData;
import org.jboss.metadata.web.spec.JspConfigMetaData;
import org.jboss.metadata.web.spec.TaglibMetaData;
import org.jboss.metadata.web.spec.Web22MetaData;
import org.jboss.metadata.web.spec.Web23MetaData;
import org.jboss.metadata.web.spec.Web24MetaData;
import org.jboss.metadata.web.spec.Web25MetaData;
import org.jboss.metadata.web.spec.Web30MetaData;
import org.jboss.metadata.web.spec.WebMetaData;


/**
 * @author Remy Maucherat
 */
public class WebMetaDataParser extends MetaDataElementParser {

    public static WebMetaData parse(XMLStreamReader reader) throws XMLStreamException {

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
                    // so assume Servlet 2.3 rather than 2.2
                    version = Version.SERVLET_2_3;
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
            if ("2.4".equals(versionString)) {
                version = Version.SERVLET_2_4;
            } else if ("2.5".equals(versionString)) {
                version = Version.SERVLET_2_5;
            } else if ("3.0".equals(versionString)) {
                version = Version.SERVLET_3_0;
            }
        }
        if (version == null) {
            // It is likely an error to not have a version at this point though
            version = Version.SERVLET_3_0;
        }
        WebMetaData wmd = null;
        switch (version) {
            case SERVLET_2_2: wmd = new Web22MetaData(); break;
            case SERVLET_2_3: wmd = new Web23MetaData(); break;
            case SERVLET_2_4: wmd = new Web24MetaData(); break;
            case SERVLET_2_5: wmd = new Web25MetaData(); break;
            case SERVLET_3_0: wmd = new Web30MetaData(); break;
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
                    wmd.setId(value);
                    break;
                }
                case VERSION: {
                    wmd.setVersion(value);
                    break;
                }
                case METADATA_COMPLETE: {
                    if (wmd instanceof Web25MetaData || wmd instanceof Web30MetaData) {
                        if (Boolean.TRUE.equals(Boolean.valueOf(value))) {
                            if (wmd instanceof Web25MetaData) {
                                ((Web25MetaData) wmd).setMetadataComplete(true);
                            }
                            if (wmd instanceof Web30MetaData) {
                                ((Web30MetaData) wmd).setMetadataComplete(true);
                            }
                        }
                    } else {
                        throw unexpectedAttribute(reader, i);
                    }
                    break;
                }
                default: throw unexpectedAttribute(reader, i);
            }
        }

        DescriptionGroupMetaData descriptionGroup = new DescriptionGroupMetaData();
        EnvironmentRefsGroupMetaData env = new EnvironmentRefsGroupMetaData();
        // Handle elements
        while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
            if (WebCommonMetaDataParser.parse(reader, wmd)) {
                continue;
            }
            if (EnvironmentRefsGroupMetaDataParser.parse(reader, env)) {
                if (wmd.getJndiEnvironmentRefsGroup() == null) {
                    wmd.setJndiEnvironmentRefsGroup(env);
                }
                continue;
            }
            if (DescriptionGroupMetaDataParser.parse(reader, descriptionGroup)) {
                if (wmd.getDescriptionGroup() == null) {
                    wmd.setDescriptionGroup(descriptionGroup);
                }
                continue;
            }
            final Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case ABSOLUTE_ORDERING:
                    if (wmd instanceof Web30MetaData) {
                        ((Web30MetaData) wmd).setAbsoluteOrdering(AbsoluteOrderingMetaDataParser.parse(reader));
                    } else {
                        throw unexpectedElement(reader);
                    }
                    break;
                case MODULE_NAME:
                    if (wmd instanceof Web30MetaData) {
                        ((Web30MetaData) wmd).setModuleName(reader.getElementText());
                    } else {
                        throw unexpectedElement(reader);
                    }
                    break;
                case TAGLIB:
                    if (wmd instanceof Web22MetaData || wmd instanceof Web23MetaData) {
                        JspConfigMetaData jspConfig = wmd.getJspConfig();
                        if (jspConfig == null) {
                            jspConfig = new JspConfigMetaData();
                            wmd.setJspConfig(jspConfig);
                        }
                        List<TaglibMetaData> taglibs = jspConfig.getTaglibs();
                        if (taglibs == null) {
                            taglibs = new ArrayList<TaglibMetaData>();
                            jspConfig.setTaglibs(taglibs);
                        }
                        taglibs.add(TaglibMetaDataParser.parse(reader));
                    } else {
                        throw unexpectedElement(reader);
                    }
                    break;
                default: throw unexpectedElement(reader);
            }
        }

        return wmd;
    }

}
