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

import java.util.Objects;
import org.jboss.as.test.integration.management.util.CLIWrapper;

/**
 * Helper abstract parent Elytron constant-* resources with one "constant" attribute.
 *
 * @author Josef Cacek
 */
public abstract class AbstractConstantHelper extends AbstractConfigurableElement {

    private final String constant;

    protected AbstractConstantHelper(Builder<?> builder) {
        super(builder);
        this.constant = Objects.requireNonNull(builder.constant, "Constant has to be provided");
    }

    @Override
    public void create(CLIWrapper cli) throws Exception {
        cli.sendLine(String.format("/subsystem=elytron/%s=%s:add(constant=\"%s\")", getConstantElytronType(), name, constant));
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        cli.sendLine(String.format("/subsystem=elytron/%s=%s:remove()", getConstantElytronType(), name));
    }

    /**
     * Returns elytron node name (e.g. for resource /subsystem=elytron/constant-principal-transformer resources it returns
     * "constant-principal-transformer").
     */
    protected abstract String getConstantElytronType();

    /**
     * Builder to build {@link AbstractConstantHelper}.
     */
    public abstract static class Builder<T extends Builder<T>> extends AbstractConfigurableElement.Builder<T> {
        private String constant;

        protected Builder() {
        }

        public T withConstant(String constant) {
            this.constant = constant;
            return self();
        }
    }
}
