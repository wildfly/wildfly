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

import org.jboss.dmr.ModelNode;

/**
 * Common parent for mechanism-realm-configuration and mechanism-configurations.
 *
 * @author Josef Cacek
 */
public class AbstractMechanismConfiguration {

    private final String preRealmPrincipalTransformer;
    private final String postRealmPrincipalTransformer;
    private final String finalPrincipalTransformer;
    private final String realmMapper;

    protected AbstractMechanismConfiguration(Builder<?> builder) {
        this.preRealmPrincipalTransformer = builder.preRealmPrincipalTransformer;
        this.postRealmPrincipalTransformer = builder.postRealmPrincipalTransformer;
        this.finalPrincipalTransformer = builder.finalPrincipalTransformer;
        this.realmMapper = builder.realmMapper;
    }

    public String getPreRealmPrincipalTransformer() {
        return preRealmPrincipalTransformer;
    }

    public String getPostRealmPrincipalTransformer() {
        return postRealmPrincipalTransformer;
    }

    public String getFinalPrincipalTransformer() {
        return finalPrincipalTransformer;
    }

    public String getRealmMapper() {
        return realmMapper;
    }

    protected ModelNode toModelNode() {
        final ModelNode node= new ModelNode();
        setIfNotNull(node, "pre-realm-principal-transformer", preRealmPrincipalTransformer);
        setIfNotNull(node, "post-realm-principal-transformer", postRealmPrincipalTransformer);
        setIfNotNull(node, "final-principal-transformer", finalPrincipalTransformer);
        setIfNotNull(node, "realm-mapper", realmMapper);
        return node;
    }

    /**
     * Builder to build {@link AbstractMechanismConfiguration}.
     */
    public abstract static class Builder<T extends Builder<T>> {
        private String preRealmPrincipalTransformer;
        private String postRealmPrincipalTransformer;
        private String finalPrincipalTransformer;
        private String realmMapper;

        protected Builder() {
        }

        protected abstract T self();

        public T withPreRealmPrincipalTransformer(String preRealmPrincipalTransformer) {
            this.preRealmPrincipalTransformer = preRealmPrincipalTransformer;
            return self();
        }

        public T withPostRealmPrincipalTransformer(String postRealmPrincipalTransformer) {
            this.postRealmPrincipalTransformer = postRealmPrincipalTransformer;
            return self();
        }

        public T withFinalPrincipalTransformer(String finalPrincipalTransformer) {
            this.finalPrincipalTransformer = finalPrincipalTransformer;
            return self();
        }

        public T withRealmMapper(String realmMapper) {
            this.realmMapper = realmMapper;
            return self();
        }
    }
}
