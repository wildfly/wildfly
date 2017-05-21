/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.security.common.elytron;

import static org.wildfly.test.security.common.ModelNodeUtil.setIfNotNull;

import org.jboss.dmr.ModelNode;

/**
 * Represantation of an attribute-mapping configuration in ldap-realm/identity-mapping.
 *
 * @author Josef Cacek
 */
public class AttributeMapping implements ModelNodeConvertable {

    private final String from;
    private final String to;
    private final String reference;
    private final String filter;
    private final String filterBaseDn;
    private final Boolean searchRecursive;
    private final Integer roleRecursion;
    private final String roleRecursionName;
    private final String extractRdn;


    private AttributeMapping(Builder builder) {
        this.from = builder.from;
        this.to = builder.to;
        this.reference = builder.reference;
        this.filter = builder.filter;
        this.filterBaseDn = builder.filterBaseDn;
        this.searchRecursive = builder.searchRecursive;
        this.roleRecursion = builder.roleRecursion;
        this.roleRecursionName = builder.roleRecursionName;
        this.extractRdn = builder.extractRdn;
    }

    @Override
    public ModelNode toModelNode() {
        ModelNode modelNode = new ModelNode();
        setIfNotNull(modelNode, "from", from);
        setIfNotNull(modelNode, "to", to);
        setIfNotNull(modelNode, "reference", reference);
        setIfNotNull(modelNode, "filter", filter);
        setIfNotNull(modelNode, "filter-base-dn", filterBaseDn);
        setIfNotNull(modelNode, "search-recursive", searchRecursive);
        setIfNotNull(modelNode, "role-recursion", roleRecursion);
        setIfNotNull(modelNode, "role-recursion-name", roleRecursionName);
        setIfNotNull(modelNode, "extract-rdn", extractRdn);
        return modelNode;
    }

    /**
     * Creates builder to build {@link AttributeMapping}.
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }
    /**
     * Builder to build {@link AttributeMapping}.
     */
    public static final class Builder {
        private String from;
        private String to;
        private String reference;
        private String filter;
        private String filterBaseDn;
        private Boolean searchRecursive;
        private Integer roleRecursion;
        private String roleRecursionName;
        private String extractRdn;

        private Builder() {
        }

        public Builder withFrom(String from) {
            this.from = from;
            return this;
        }

        public Builder withTo(String to) {
            this.to = to;
            return this;
        }

        public Builder withReference(String reference) {
            this.reference = reference;
            return this;
        }

        public Builder withFilter(String filter) {
            this.filter = filter;
            return this;
        }

        public Builder withFilterBaseDn(String filterBaseDn) {
            this.filterBaseDn = filterBaseDn;
            return this;
        }

        public Builder withSearchRecursive(Boolean searchRecursive) {
            this.searchRecursive = searchRecursive;
            return this;
        }

        public Builder withRoleRecursion(Integer roleRecursion) {
            this.roleRecursion = roleRecursion;
            return this;
        }

        public Builder withRoleRecursionName(String roleRecursionName) {
            this.roleRecursionName = roleRecursionName;
            return this;
        }

        public Builder withExtractRdn(String extractRdn) {
            this.extractRdn = extractRdn;
            return this;
        }

        public AttributeMapping build() {
            return new AttributeMapping(this);
        }
    }
}
