/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
