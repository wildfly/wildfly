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

/**
 * Elytron constant-principal-decoder configuration implementation.
 *
 * @author Ondrej Kotek
 */
public class ConstantPrincipalDecoder extends AbstractConstantHelper {

    private ConstantPrincipalDecoder(Builder builder) {
        super(builder);
    }


    @Override
    protected String getConstantElytronType() {
        return "constant-principal-decoder";
    }

    /**
     * Creates builder.
     *
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder pattern for the class.
     */
    public static final class Builder extends AbstractConstantHelper.Builder<Builder> {

        private Builder() {
        }

        public ConstantPrincipalDecoder build() {
            return new ConstantPrincipalDecoder(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }

}
