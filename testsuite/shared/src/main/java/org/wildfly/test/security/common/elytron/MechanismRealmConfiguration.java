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

import java.util.Objects;

import org.jboss.dmr.ModelNode;

/**
 * Object which holds single instance from mechanism-realm-configurations list.
 *
 * @author Josef Cacek
 */
public class MechanismRealmConfiguration extends AbstractMechanismConfiguration {

    private final String realmName;

    private MechanismRealmConfiguration(Builder builder) {
        super(builder);
        this.realmName = Objects.requireNonNull(builder.realmName, "Realm name must not be null.");
    }

    public String getRealmName() {
        return realmName;
    }

    /**
     * Creates builder to build {@link MechanismRealmConfiguration}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }


    @Override
    public ModelNode toModelNode() {
        ModelNode node = super.toModelNode();
        setIfNotNull(node, "realm-name", realmName);
        return node;
    }

    /**
     * Builder to build {@link MechanismRealmConfiguration}.
     */
    public static final class Builder extends AbstractMechanismConfiguration.Builder<Builder> {
        private String realmName;

        private Builder() {
        }

        public Builder withRealmName(String realmName) {
            this.realmName = realmName;
            return this;
        }

        public MechanismRealmConfiguration build() {
            return new MechanismRealmConfiguration(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

}
