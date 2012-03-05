/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.cli.CliConfig;
import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.SSLConfig;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLMapper;

/**
 * Represents the JBoss CLI configuration.
 *
 * @author Alexey Loubyansky
 */
class CliConfigImpl implements CliConfig {

    private static final String NS = "urn:jboss:cli:1.0";
    private static final String DEFAULT_CONTROLLER = "default-controller";
    private static final String ENABLED = "enabled";
    private static final String FILE_DIR = "file-dir";
    private static final String FILE_NAME = "file-name";
    private static final String JBOSS_CLI = "jboss-cli";
    private static final String HISTORY = "history";
    private static final String HOST = "host";
    private static final String MAX_SIZE = "max-size";
    private static final String PORT = "port";

    static CliConfig load(final CommandContext ctx) throws CliInitializationException {
        final String jbossHome = SecurityActions.getEnvironmentVariable("JBOSS_HOME");
        if(jbossHome == null) {
            System.err.println("WARN: can't load the config file because JBOSS_HOME environment variable is not set.");
            return new CliConfigImpl();
        }
        return parse(ctx, new File(jbossHome + File.separatorChar + "bin", "jboss-cli.xml"));
    }

    static CliConfig parse(final CommandContext ctx, File f) throws CliInitializationException {
        if(f == null) {
            throw new CliInitializationException("The file argument is null.");
        }
        if(!f.exists()) {
            //throw new CliInitializationException(f.getAbsolutePath() + " doesn't exist.");
            return new CliConfigImpl();
        }

        CliConfigImpl config = new CliConfigImpl();

        BufferedInputStream input = null;
        try {
            final XMLMapper mapper = XMLMapper.Factory.create();
            mapper.registerRootElement(new QName(NS, JBOSS_CLI), new XMLElementReader<CliConfigImpl>(){
                @Override
                public void readElement(XMLExtendedStreamReader reader, CliConfigImpl config) throws XMLStreamException {
                    boolean jbossCliEnded = false;
                    while (reader.hasNext() && jbossCliEnded == false) {
                        int tag = reader.nextTag();
                        if(tag == XMLStreamConstants.START_ELEMENT) {
                            final String localName = reader.getLocalName();
                            if(localName.equals(DEFAULT_CONTROLLER)) {
                                readDefaultController(reader, config);
                            } else if(localName.equals(HISTORY)) {
                                readHistory(reader, config);
                            } else if (localName.equals("ssl")) {
                                SslConfig sslConfig = new SslConfig();
                                readSSLElement(reader, sslConfig);
                                config.sslConfig = sslConfig;
                            } else {
                                throw new XMLStreamException("Unexpected element: " + localName);
                            }
                        } else if(tag == XMLStreamConstants.END_ELEMENT) {
                            final String localName = reader.getLocalName();
                            if (localName.equals(JBOSS_CLI)) {
                                jbossCliEnded = true;
                            }
                        }
                    }
                }

                private void readDefaultController(XMLExtendedStreamReader reader, CliConfigImpl config) throws XMLStreamException {
                    while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                        final String localName = reader.getLocalName();
                        final String resolved = resolveString(reader.getElementText());
                        if (HOST.equals(localName)) {
                            config.defaultControllerHost = resolved;
                        } else if (PORT.equals(localName)) {
                            try {
                                config.defaultControllerPort = Integer.parseInt(resolved);
                            } catch(NumberFormatException e) {
                                throw new XMLStreamException("Failed to parse " + DEFAULT_CONTROLLER + " " + PORT + " value '" + resolved + "'", e);
                            }
                        } else {
                            throw new XMLStreamException("Unexpected child of " + DEFAULT_CONTROLLER + ": " + localName);
                        }
                    }
                }

                private void readHistory(XMLExtendedStreamReader reader, CliConfigImpl config) throws XMLStreamException {
                    while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                        final String localName = reader.getLocalName();
                        final String resolved = resolveString(reader.getElementText());
                        if (ENABLED.equals(localName)) {
                            config.historyEnabled = Boolean.parseBoolean(resolved);
                        } else if (FILE_NAME.equals(localName)) {
                            config.historyFileName = resolved;
                        } else if (FILE_DIR.equals(localName)) {
                            config.historyFileDir = resolved;
                        } else if (MAX_SIZE.equals(localName)) {
                            try {
                                config.historyMaxSize = Integer.parseInt(resolved);
                            } catch(NumberFormatException e) {
                                throw new XMLStreamException("Failed to parse " + HISTORY + " " + MAX_SIZE + " value '" + resolved + "'", e);
                            }
                        } else {
                            throw new XMLStreamException("Unexpected child of " + DEFAULT_CONTROLLER + ": " + localName);
                        }
                    }
                }

                public void readSSLElement(XMLExtendedStreamReader reader, SslConfig config) throws XMLStreamException {
                    while (reader.hasNext() && reader.nextTag() != END_ELEMENT) {
                        final String localName = reader.getLocalName();
                        if ("keyStore".equals(localName)) {
                            config.setKeyStore(reader.getElementText());
                        } else if ("keyStorePassword".equals(localName)) {
                            config.setKeyStorePassword(reader.getElementText());
                        } else if ("trustStore".equals(localName)) {
                            config.setTrustStore(reader.getElementText());
                        } else if ("trustStorePassword".equals(localName)) {
                            config.setTrustStorePassword(reader.getElementText());
                        } else if ("modifyTrustStore".equals(localName)) {
                            config.setModifyTrustStore(Boolean.getBoolean(reader.getElementText()));
                        }
                    }
                }
            });

            FileInputStream is = new FileInputStream(f);
            input = new BufferedInputStream(is);
            XMLStreamReader streamReader = XMLInputFactory.newInstance().createXMLStreamReader(input);
            mapper.parseDocument(config, streamReader);
            streamReader.close();
        } catch(Throwable t) {
            throw new CliInitializationException("Failed to parse " + f.getAbsolutePath(), t);
        } finally {
            StreamUtils.safeClose(input);
        }
        return config;
    }

    private static String resolveString(String str) throws XMLStreamException {
        if(str == null) {
            return null;
        }
        if(str.startsWith("${") && str.endsWith("}")) {
            str = str.substring(2, str.length() - 1);
            final String resolved = SecurityActions.getSystemProperty(str);
            if(resolved == null) {
                throw new XMLStreamException("Failed to resolve '" + str + "' to a non-null value.");
            }
            str = resolved;
        }
        return str;
    }

    private CliConfigImpl() {
        defaultControllerHost = "localhost";
        defaultControllerPort = 9999;

        historyEnabled = true;
        historyFileName = ".jboss-cli-history";
        historyFileDir = SecurityActions.getSystemProperty("user.home");
        historyMaxSize = 500;
    }

    private String defaultControllerHost;
    private int defaultControllerPort;

    private boolean historyEnabled;
    private String historyFileName;
    private String historyFileDir;
    private int historyMaxSize;

    private SSLConfig sslConfig;

    @Override
    public String getDefaultControllerHost() {
        return defaultControllerHost;
    }

    @Override
    public int getDefaultControllerPort() {
        return defaultControllerPort;
    }

    @Override
    public boolean isHistoryEnabled() {
        return historyEnabled;
    }

    @Override
    public String getHistoryFileName() {
        return historyFileName;
    }

    @Override
    public String getHistoryFileDir() {
        return historyFileDir;
    }

    @Override
    public int getHistoryMaxSize() {
        return historyMaxSize;
    }

    @Override
    public SSLConfig getSslConfig() {
        return sslConfig;
    }

    static class SslConfig implements SSLConfig {

        private String keyStore = null;
        private String keyStorePassword = null;
        private String trustStore = null;
        private String trustStorePassword = null;
        private boolean modifyTrustStore = true;

        public String getKeyStore() {
            return keyStore;
        }

        void setKeyStore(final String keyStore) {
            this.keyStore = keyStore;
        }

        public String getKeyStorePassword() {
            return keyStorePassword;
        }

        void setKeyStorePassword(final String keyStorePassword) {
            this.keyStorePassword = keyStorePassword;
        }

        public String getTrustStore() {
            return trustStore;
        }

        void setTrustStore(final String trustStore) {
            this.trustStore = trustStore;
        }

        public String getTrustStorePassword() {
            return trustStorePassword;
        }

        void setTrustStorePassword(final String trustStorePassword) {
            this.trustStorePassword = trustStorePassword;
        }

        public boolean isModifyTrustStore() {
            return modifyTrustStore;
        }

        void setModifyTrustStore(final boolean modifyTrustStore) {
            this.modifyTrustStore = modifyTrustStore;
        }

    }
}