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

import org.jboss.as.test.integration.management.util.CLIWrapper;

/**
 * Elytron x500-attribute-principal-decoder configuration implementation.
 *
 * @author Ondrej Kotek
 */
public class X500AttributePrincipalDecoder extends AbstractConfigurableElement {

    private final String oid;
    private final String attributeName;
    private final Integer maximumSegments;

    private X500AttributePrincipalDecoder(Builder builder) {
        super(builder);
        this.oid = builder.oid;
        this.attributeName = builder.attributeName;
        this.maximumSegments = builder.maximumSegments;
        if ((attributeName == null && oid == null) || (attributeName != null && oid != null)) {
            throw new IllegalArgumentException("attribute-name xor oid has to be provided");
        }
    }

    @Override
    public void create(CLIWrapper cli) throws Exception {
        // /subsystem=elytron/x500-attribute-principal-decoder=test:add(oid=2.5.4.3,maximum-segments=1)
        StringBuilder command = new StringBuilder("/subsystem=elytron/x500-attribute-principal-decoder=").append(name)
                .append(":add(");

        if (oid != null) {
            command.append("oid=").append(oid);
        } else {
            command.append("attribute-name=").append(attributeName);
        }
        if (maximumSegments != null) {
            command.append(",maximum-segments=").append(maximumSegments);
        }

        command.append(")");

        cli.sendLine(command.toString());
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        cli.sendLine(String.format("/subsystem=elytron/x500-attribute-principal-decoder=%s:remove()", name));
    }

    /**
     * Creates builder to build {@link X500AttributePrincipalDecoder}.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to build {@link X500AttributePrincipalDecoder}.
     */
    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {
        private String oid;
        private String attributeName;
        private Integer maximumSegments;

        private Builder() {
        }

        /**
         * Uses attribute with given object id to construct principal name. E.g. oid "2.5.4.3" ~ CN (Common Name).
         * {@link #withAttributeName(java.lang.String)} can be used instead.
         * @param oid object identifier
         * @return builder
         */
        public Builder withOid(String oid) {
            this.oid = oid;
            return this;
        }

        public Builder withAttributeName(String attributeName) {
            this.attributeName = attributeName;
            return this;
        }

        public Builder withMaximumSegments(int maximumSegments) {
            this.maximumSegments = maximumSegments;
            return this;
        }

        public X500AttributePrincipalDecoder build() {
            return new X500AttributePrincipalDecoder(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
