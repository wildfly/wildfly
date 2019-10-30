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
 * Object which holds sasl filter configuration.
 *
 * @author Josef Cacek
 */
public class SaslFilter {

    private final String predefinedFilter;
    private final String patternFilter;
    private final Boolean enabling;

    private SaslFilter(Builder builder) {
        this.predefinedFilter = builder.predefinedFilter;
        this.patternFilter = builder.patternFilter;
        this.enabling = builder.enabling;
    }

    /**
     * Creates builder to build {@link SaslFilter}.
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public String getPredefinedFilter() {
        return predefinedFilter;
    }

    public String getPatternFilter() {
        return patternFilter;
    }

    public Boolean isEnabling() {
        return enabling;
    }

    /**
     * Builder to build {@link SaslFilter}.
     */
    public static final class Builder {
        private String predefinedFilter;
        private String patternFilter;
        private Boolean enabling;

        private Builder() {
        }

        public Builder withPredefinedFilter(String predefinedFilter) {
            this.predefinedFilter = predefinedFilter;
            return this;
        }

        public Builder withPatternFilter(String patternFilter) {
            this.patternFilter = patternFilter;
            return this;
        }

        public Builder withEnabling(Boolean enabling) {
            this.enabling = enabling;
            return this;
        }

        public SaslFilter build() {
            return new SaslFilter(this);
        }
    }


}
