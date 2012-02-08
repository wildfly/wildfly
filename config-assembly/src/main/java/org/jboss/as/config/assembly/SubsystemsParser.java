/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.config.assembly;

import static javax.xml.stream.XMLStreamConstants.END_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_DOCUMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class SubsystemsParser {

    private final File inputFile;
    Map<String, SubsystemConfig[]> subsystemConfigs = new HashMap<String, SubsystemConfig[]>();

    SubsystemsParser(final File inputFile) {
        this.inputFile = inputFile;
    }

    Map<String, SubsystemConfig[]> getSubsystemConfigs() {
        return subsystemConfigs;
    }

    void parse() throws IOException, XMLStreamException {
        InputStream in = new BufferedInputStream(new FileInputStream(inputFile));
        try {
            XMLStreamReader reader = XMLInputFactory.newInstance().createXMLStreamReader(in);
            reader.require(START_DOCUMENT, null, null);
            boolean done = false;
            while (reader.hasNext()) {
                int type = reader.next();
                switch (type) {
                case START_ELEMENT:
                    if (!done && reader.getLocalName().equals("config")) {
                        parseSubsystems(reader);
                        done = true;
                    } else {
                        throw new XMLStreamException("Invalid element " + reader.getLocalName(), reader.getLocation());
                    }
                    break;
                case END_DOCUMENT:
                    return;
                }
            }

        } finally {
            try {
                in.close();
            } catch (Exception ignore) {
            }
        }
    }

    private void parseSubsystems(XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            int type = reader.next();
            switch (type) {
            case START_ELEMENT:
                if (reader.getLocalName().equals("subsystems")) {
                    String name = "";
                    for (int i = 0 ; i < reader.getAttributeCount() ; i++) {
                        if (!reader.getAttributeLocalName(i).equals("name")) {
                            throw new XMLStreamException("Unknown attribute " + reader.getAttributeLocalName(i), reader.getLocation());
                        } else {
                            name = reader.getAttributeValue(i);
                        }
                    }
                    if (subsystemConfigs.containsKey(name)) {
                        throw new XMLStreamException("Already have a subsystems named " + name, reader.getLocation());
                    }
                    List<SubsystemConfig> subsystems = new ArrayList<SubsystemConfig>();
                    parseSubsystem(reader, subsystems);
                    this.subsystemConfigs.put(name, subsystems.toArray(new SubsystemConfig[subsystems.size()]));
                } else {
                    throw new XMLStreamException("Invalid element " + reader.getLocalName(), reader.getLocation());
                }
                break;
            case END_ELEMENT:
                if (reader.getLocalName().equals("config")) {
                    return;
                }
            }
        }
    }

    private void parseSubsystem(XMLStreamReader reader, List<SubsystemConfig> subsystems) throws XMLStreamException {
        reader.next();
        while (true) {
            switch (reader.getEventType()) {
            case START_ELEMENT:
                if (reader.getLocalName().equals("subsystem")) {
                    String supplement = null;
                    for (int i = 0 ; i < reader.getAttributeCount() ; i++) {
                        String attr = reader.getAttributeLocalName(i);
                        if (attr.equals("supplement")) {
                            supplement = reader.getAttributeValue(i);
                        } else {
                            throw new XMLStreamException("Unknown attribute " + attr, reader.getLocation());
                        }

                    }
                    subsystems.add(new SubsystemConfig(reader.getElementText(), supplement));
                } else {
                    throw new XMLStreamException("Invalid element " + reader.getLocalName(), reader.getLocation());
                }
                break;
            case END_ELEMENT:
                if (reader.getLocalName().equals("subsystems")) {
                    return;
                }
            default:
                reader.next();
            }
        }
    }
}
