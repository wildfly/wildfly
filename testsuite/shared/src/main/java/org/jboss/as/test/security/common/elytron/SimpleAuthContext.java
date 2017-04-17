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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.security.common.elytron;

import org.jboss.as.test.integration.management.util.CLIWrapper;

/**
 * Elytron authentication context configuration implementation.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class SimpleAuthContext extends AbstractConfigurableElement {

    private final String extendsAttr;
    private final MatchRules matchRules;

    private SimpleAuthContext(final Builder builder) {
        super(builder);
        this.extendsAttr = builder.extendsAttr;
        this.matchRules = builder.matchRules;
    }

    @Override
    public void create(CLIWrapper cli) throws Exception {

        final StringBuilder sb = new StringBuilder("/subsystem=elytron/authentication-context=\"");
        sb.append(name).append("\":add(");
        if (extendsAttr != null && !extendsAttr.isEmpty()) {
            sb.append(String.format("extends=\"%s\", ", extendsAttr));
        }
        if (matchRules != null) {
            sb.append(matchRules.asString());
        }
        sb.append(")");
        cli.sendLine(sb.toString());
    }

    @Override
    public void remove(CLIWrapper cli) throws Exception {
        cli.sendLine(String.format("/subsystem=elytron/authentication-context=\"%s\":remove", name));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends AbstractConfigurableElement.Builder<Builder> {
        private String extendsAttr;
        private MatchRules matchRules;

        private Builder() {
        }

        public Builder withExtends(final String extendsAttr) {
            this.extendsAttr = extendsAttr;
            return this;
        }

        public Builder withMatchRules(final MatchRules matchRules) {
            this.matchRules = matchRules;
            return this;
        }

        public SimpleAuthContext build() {
            return new SimpleAuthContext(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
