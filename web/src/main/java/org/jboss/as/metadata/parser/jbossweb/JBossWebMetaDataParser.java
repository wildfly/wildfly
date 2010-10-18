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

package org.jboss.as.metadata.parser.jbossweb;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.metadata.parser.util.MetaDataElementParser;
import org.jboss.metadata.web.jboss.JBoss4xDTDWebMetaData;
import org.jboss.metadata.web.jboss.JBoss50DTDWebMetaData;
import org.jboss.metadata.web.jboss.JBoss50WebMetaData;
import org.jboss.metadata.web.jboss.JBoss60WebMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;


/**
 * @author Remy Maucherat
 */
public class JBossWebMetaDataParser extends MetaDataElementParser {

    public static JBossWebMetaData parse(XMLStreamReader reader) throws XMLStreamException {

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
                    // so assume jboss-web 5.0
                    version = Version.JBOSS_WEB_5_0;
                }
            }
        }
        String schemaLocation = readSchemaLocation(reader);
        if (schemaLocation != null) {
            version = Location.getVersion(schemaLocation);
        }
        if (version == null) {
            version = Version.JBOSS_WEB_6_0;
        }
        JBossWebMetaData wmd = null;
        switch (version) {
            case JBOSS_WEB_3_0: wmd = new JBoss4xDTDWebMetaData(); break;
            case JBOSS_WEB_3_2: wmd = new JBoss4xDTDWebMetaData(); break;
            case JBOSS_WEB_4_0: wmd = new JBoss4xDTDWebMetaData(); break;
            case JBOSS_WEB_4_2: wmd = new JBoss4xDTDWebMetaData(); break;
            case JBOSS_WEB_5_0: wmd = new JBoss50DTDWebMetaData(); break;
            case JBOSS_WEB_5_1: wmd = new JBoss50WebMetaData(); break;
            case JBOSS_WEB_6_0: wmd = new JBoss60WebMetaData(); break;
        }

        return wmd;
    }

}
