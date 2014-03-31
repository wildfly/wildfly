/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.cli.impl;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.protocol.StreamUtils;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLMapper;


/**
 * @author Alexey Loubyansky
 *
 */
class VaultConfig {

    private String code;
    private String module;
    private final Map<String, Object> options = new HashMap<String, Object>();

    static VaultConfig load(File f) throws XMLStreamException {
        if(f == null) {
            throw new IllegalArgumentException("File is null");
        }
        if(!f.exists()) {
            throw new XMLStreamException("Failed to locate vault file " + f.getAbsolutePath());
        }

        final VaultConfig config = new VaultConfig();
        BufferedInputStream input = null;
        try {
            final XMLMapper mapper = XMLMapper.Factory.create();
            final XMLElementReader<VaultConfig> reader = new VaultConfigReader();
            mapper.registerRootElement(new QName(VaultConfigReader.VAULT), reader);
            FileInputStream is = new FileInputStream(f);
            input = new BufferedInputStream(is);
            XMLStreamReader streamReader = XMLInputFactory.newInstance().createXMLStreamReader(input);
            mapper.parseDocument(config, streamReader);
            streamReader.close();
        } catch(FileNotFoundException e) {
            throw new XMLStreamException("Vault file not found", e);
        } catch(XMLStreamException t) {
            throw t;
        } finally {
            StreamUtils.safeClose(input);
        }
        return config;
    }

    Map<String, Object> getOptions() {
        return options;
    }

    String getCode() {
        return code;
    }

    String getModule() {
        return module;
    }

    void addOption(String name, String value) {
        if(name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name is null or empty");
        }
        if(value == null || value.isEmpty()) {
            throw new IllegalArgumentException("value is null or empty");
        }
        options.put(name, value);
    }

    static class VaultConfigReader implements XMLElementReader<VaultConfig> {

        static final String NAME = "name";
        static final String VALUE = "value";
        static final String VAULT = "vault";
        static final String CODE = "code";
        static final String MODULE = "module";
        static final String VAULT_OPTION = "vault-option";

        @Override
        public void readElement(XMLExtendedStreamReader reader, VaultConfig config) throws XMLStreamException {

            String rootName = reader.getLocalName();
            if (VAULT.equals(rootName) == false) {
                throw new XMLStreamException("Unexpected element: " + rootName);
            }

            Namespace ns = Namespace.forUri(reader.getNamespaceURI());
            boolean allowOverrideImpl = ns.compareTo(Namespace.CLI_2_1) >= 0;
            final int count = reader.getAttributeCount();
            for (int i = 0; i < count; i++) {
                final String value = reader.getAttributeValue(i);
                if (!allowOverrideImpl){
                    unexpectedVaultAttribute(reader.getAttributeLocalName(i), reader);
                } else {
                    String name = reader.getAttributeLocalName(i);
                    if (name.equals(CODE)){
                        config.code = value;
                    } else if (name.equals(MODULE)){
                        config.module = value;
                    } else {
                        unexpectedVaultAttribute(reader.getAttributeLocalName(i), reader);
                    }
                }
            }
            if (config.code == null && config.module != null){
                throw new XMLStreamException("Attribute 'module' was specified without a 'module' " +
                        " for element '" +
                        VAULT_OPTION + "' at " + reader.getLocation());
            }

            boolean done = false;
            while (reader.hasNext() && done == false) {
                int tag = reader.nextTag();
                if(tag == XMLStreamConstants.START_ELEMENT) {
                    final String localName = reader.getLocalName();
                    if (localName.equals(VAULT_OPTION)) {
                        final String name = reader.getAttributeValue(null, NAME);
                        if(name == null) {
                            throw new XMLStreamException("Attribute '" + NAME +
                                    "' is not found for element '" +
                                    VAULT_OPTION + "' at " + reader.getLocation());
                        }
                        final String value = reader.getAttributeValue(null, VALUE);
                        if(value == null) {
                            throw new XMLStreamException("Attribute '" + VALUE +
                                    "' is not found for element " +
                                    VAULT_OPTION + "' at " + reader.getLocation());
                        }
                        config.addOption(name.trim(), value.trim());
                        CliConfigImpl.CliConfigReader.requireNoContent(reader);
                    } else {
                        throw new XMLStreamException("Unexpected element: " + localName);
                    }
                } else if(tag == XMLStreamConstants.END_ELEMENT) {
                    final String localName = reader.getLocalName();
                    if (localName.equals(VAULT)) {
                        done = true;
                    }
                }
            }
        }

        private void unexpectedVaultAttribute(String attribute, XMLStreamReader reader) throws XMLStreamException {
            throw new XMLStreamException("Attribute '" + attribute +
                    "' is unknown for element '" +
                    VAULT_OPTION + "' at " + reader.getLocation());

        }
    }
}
