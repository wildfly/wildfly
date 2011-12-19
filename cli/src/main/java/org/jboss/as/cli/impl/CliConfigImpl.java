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
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.jboss.as.cli.CliConfig;
import org.jboss.as.cli.CliInitializationException;
import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.SSLConfig;
import org.jboss.as.cli.operation.impl.ConcurrentRolloutPlanGroup;
import org.jboss.as.cli.operation.impl.RolloutPlanHeader;
import org.jboss.as.cli.operation.impl.SingleRolloutPlanGroup;
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
            mapper.registerRootElement(new QName("urn:jboss:cli:1.0", "jboss-cli"), new XMLElementReader<CliConfigImpl>(){
                @Override
                public void readElement(XMLExtendedStreamReader reader, CliConfigImpl config) throws XMLStreamException {

                    RolloutPlanHeader rolloutPlan = null;
                    boolean concurrent = false;
                    boolean jbossCliEnded = false;
                    while (reader.hasNext() && jbossCliEnded == false) {
                        int tag = reader.nextTag();
                        if(tag == XMLStreamConstants.START_ELEMENT) {
                            final String localName = reader.getLocalName();
                            if(localName.equals("plan")) {
                                final String planName = reader.getAttributeValue(null, "id");
                                if(planName == null) {
                                    throw new IllegalStateException("Rollout plan is missing required attribute 'id' @" + reader.getLocation().getColumnNumber() + "," + reader.getLocation().getLineNumber());
                                }
                                rolloutPlan = new RolloutPlanHeader(planName);
                                config.addRolloutPlan(rolloutPlan);
                            } else if(localName.equals("concurrent")) {
                                concurrent = true;
                                rolloutPlan.addGroup(new ConcurrentRolloutPlanGroup());
                            } else if(localName.equals("server-group")) {
                                final String name = reader.getAttributeValue(null, "name");
                                if(name == null) {
                                    throw new IllegalStateException("Server group is missing required attribute 'name' @" + reader.getLocation().getColumnNumber() + "," + reader.getLocation().getLineNumber());
                                }
                                final SingleRolloutPlanGroup group = new SingleRolloutPlanGroup();
                                group.setGroupName(name);
                                String value = reader.getAttributeValue(null, "rolling-to-servers");
                                if(value != null) {
                                    group.addProperty("rolling-to-servers", value);
                                }
                                value = reader.getAttributeValue(null, "max-failure-percentage");
                                if(value != null) {
                                    group.addProperty("max-failure-percentage", value);
                                }
                                value = reader.getAttributeValue(null, "max-failed-servers");
                                if(value != null) {
                                    group.addProperty("max-failed-servers", value);
                                }
                                if(concurrent) {
                                    rolloutPlan.addConcurrentGroup(group);
                                } else {
                                    rolloutPlan.addGroup(group);
                                }
                            } else if (localName.equals("ssl")) {
                                SslConfig sslConfig = new SslConfig();
                                readSSLElement(reader, sslConfig);
                                config.sslConfig = sslConfig;
                            }
                        } else if(tag == XMLStreamConstants.END_ELEMENT) {
                            final String localName = reader.getLocalName();
                            if(localName.equals("concurrent")) {
                                concurrent = false;
                            } else if (localName.equals("jboss-cli")) {
                                jbossCliEnded = true;
                            }
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

    private CliConfigImpl() {}

    private Map<String, RolloutPlanHeader> rolloutPlans;
    private SSLConfig sslConfig;

    public RolloutPlanHeader getRolloutPlan(String name) {
        return rolloutPlans == null ? null : rolloutPlans.get(name);
    }

    public SSLConfig getSslConfig() {
        return sslConfig;
    }

    public void addRolloutPlan(RolloutPlanHeader rolloutPlan) {
        if(rolloutPlan == null) {
            throw new IllegalArgumentException();
        }
        if(rolloutPlan.getPlanId() == null) {
            throw new IllegalArgumentException("Rollout plan is missing the name.");
        }
        if(rolloutPlans == null) {
            rolloutPlans = new HashMap<String, RolloutPlanHeader>();
        }
        if(rolloutPlans.put(rolloutPlan.getPlanId(), rolloutPlan) != null) {
            throw new IllegalArgumentException("Duplicate rollout plan name: '" + rolloutPlan.getName() + "'");
        }
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