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
package org.wildfly.build.plugin.configassembly;

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
import java.util.List;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.modules.ModuleIdentifier;

/**
 *
 * @author Thomas.Diesler@jboss.com
 * @since 06-Sep-2012
 */
class ModuleParser {

    private final File inputFile;
    List<ModuleDependency> dependencies = new ArrayList<ModuleDependency>();

    ModuleParser(final File inputFile) {
        this.inputFile = inputFile;
    }

    List<ModuleDependency> getDependencies() {
        return dependencies;
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
                        if (!done && reader.getLocalName().equals("module")) {
                            parseModule(reader);
                            done = true;
                        }
                        else if (!done && reader.getLocalName().equals("module-alias")) {
                            parseModuleAlias(reader);
                            done = true;
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

    private void parseModule(XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            int type = reader.next();
            switch (type) {
                case START_ELEMENT:
                    if (reader.getLocalName().equals("dependencies")) {
                        parseDependencies(reader);
                    }
                    break;
                case END_ELEMENT:
                    if (reader.getLocalName().equals("module")) {
                        return;
                    }
            }
        }
    }

    private void parseModuleAlias(XMLStreamReader reader) throws XMLStreamException {
        String name = "";
        String slot = "main";
        boolean optional = false;
        for (int i = 0 ; i < reader.getAttributeCount() ; i++) {
            String localName = reader.getAttributeLocalName(i);
            if (localName.equals("target-name")) {
                name = reader.getAttributeValue(i);
            } else if (localName.equals("target-slot")) {
                slot = reader.getAttributeValue(i);
            }
        }
        ModuleIdentifier moduleId = ModuleIdentifier.create(name, slot);
        dependencies.add(new ModuleDependency(moduleId, optional));
    }

    private void parseDependencies(XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            int type = reader.next();
            switch (type) {
                case START_ELEMENT:
                    if (reader.getLocalName().equals("module")) {
                        String name = "";
                        String slot = "main";
                        boolean optional = false;
                        for (int i = 0 ; i < reader.getAttributeCount() ; i++) {
                            String localName = reader.getAttributeLocalName(i);
                            if (localName.equals("name")) {
                                name = reader.getAttributeValue(i);
                            } else if (localName.equals("slot")) {
                                slot = reader.getAttributeValue(i);
                            } else if (localName.equals("optional")) {
                                optional = Boolean.parseBoolean(reader.getAttributeValue(i));
                            }
                        }
                        ModuleIdentifier moduleId = ModuleIdentifier.create(name, slot);
                        dependencies.add(new ModuleDependency(moduleId, optional));
                    }
                    break;
                case END_ELEMENT:
                    if (reader.getLocalName().equals("dependencies")) {
                        return;
                    }
            }
        }
    }

    static class ModuleDependency {
        private final ModuleIdentifier moduleId;
        private final boolean optional;

        ModuleDependency(ModuleIdentifier moduleId, boolean optional) {
            this.moduleId = moduleId;
            this.optional = optional;
        }

        ModuleIdentifier getModuleId() {
            return moduleId;
        }

        boolean isOptional() {
            return optional;
        }

        @Override
        public String toString() {
            return "[" + moduleId + (optional ? ",optional=true" : "") + "]";
        }
    }
}
