/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.security.common.elytron;

import static org.wildfly.test.security.common.ModelNodeUtil.setIfNotNull;

import java.util.ArrayList;
import java.util.List;

import org.jboss.dmr.ModelNode;

/**
 * Object which holds single instance from mechanism-configurations list.
 *
 * @author Josef Cacek
 */
public class MechanismConfiguration extends AbstractMechanismConfiguration {

    private final String mechanismName;
    private final String hostName;
    private final String protocol;
    private final String credentialSecurityFactory;
    private final List<MechanismRealmConfiguration> mechanismRealmConfigurations;

    private MechanismConfiguration(Builder builder) {
        super(builder);
        this.mechanismName = builder.mechanismName;
        this.hostName = builder.hostName;
        this.protocol = builder.protocol;
        this.credentialSecurityFactory = builder.credentialSecurityFactory;
        this.mechanismRealmConfigurations = new ArrayList<>(builder.mechanismRealmConfigurations);
    }

    public String getMechanismName() {
        return mechanismName;
    }

    public String getHostName() {
        return hostName;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getCredentialSecurityFactory() {
        return credentialSecurityFactory;
    }

    public List<MechanismRealmConfiguration> getMechanismRealmConfigurations() {
        return mechanismRealmConfigurations;
    }

    /**
     * Creates builder to build {@link MechanismConfiguration}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public ModelNode toModelNode() {
        ModelNode node = super.toModelNode();
        setIfNotNull(node, "mechanism-name", mechanismName);
        setIfNotNull(node, "host-name", hostName);
        setIfNotNull(node, "protocol", protocol);
        setIfNotNull(node, "credential-security-factory", credentialSecurityFactory);
        if (!mechanismRealmConfigurations.isEmpty()) {
            ModelNode confs = node.get("mechanism-realm-configurations");
            for (MechanismRealmConfiguration conf:mechanismRealmConfigurations) {
                confs.add(conf.toModelNode());
            }
        }
        return node;
    }

    /**
     * Builder to build {@link MechanismConfiguration}.
     */
    public static final class Builder extends AbstractMechanismConfiguration.Builder<Builder> {
        private String mechanismName;
        private String hostName;
        private String protocol;
        private String credentialSecurityFactory;
        private List<MechanismRealmConfiguration> mechanismRealmConfigurations = new ArrayList<>();

        private Builder() {
        }

        public Builder withMechanismName(String mechanismName) {
            this.mechanismName = mechanismName;
            return this;
        }

        public Builder withHostName(String hostName) {
            this.hostName = hostName;
            return this;
        }

        public Builder withProtocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public Builder withCredentialSecurityFactory(String credentialSecurityFactory) {
            this.credentialSecurityFactory = credentialSecurityFactory;
            return this;
        }

        public Builder addMechanismRealmConfiguration(MechanismRealmConfiguration mechanismRealmConfiguration) {
            this.mechanismRealmConfigurations.add(mechanismRealmConfiguration);
            return this;
        }

        public MechanismConfiguration build() {
            return new MechanismConfiguration(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
