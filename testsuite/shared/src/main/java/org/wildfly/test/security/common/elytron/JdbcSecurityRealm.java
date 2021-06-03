/*
 * Copyright 2019 Red Hat, Inc.
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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.dmr.ModelNode;

/**
 * A {@link ConfigurableElement} to define a JDBC SecurityRealm resource.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class JdbcSecurityRealm implements SecurityRealm {

    private final PathAddress address;
    private final String name;
    private final List<ModelNode> principalQueries;
    private final String hashCharset;

    JdbcSecurityRealm(final String name, final List<ModelNode> principalQueries, String hashCharset) {
        this.name = name;
        this.address = PathAddress.pathAddress(PathElement.pathElement("subsystem", "elytron"), PathElement.pathElement("jdbc-realm", name));
        this.principalQueries = principalQueries;
        this.hashCharset = hashCharset;
    }

    @Override
    public String getName() {
        return name;
    }

    public ModelNode getAddOperation() {
        ModelNode addOperation = Util.createAddOperation(address);
        addOperation.get("principal-query").set(principalQueries);
        addOperation.get("hash-charset").set(hashCharset);

        return addOperation;
    }

    public ModelNode getRemoveOperation() {
        return Util.createRemoveOperation(address);
    }

    @Override
    public void create(ModelControllerClient client, CLIWrapper cli) throws Exception {
        Utils.applyUpdate(getAddOperation(), client);
    }

    @Override
    public void remove(ModelControllerClient client, CLIWrapper cli) throws Exception {
        Utils.applyUpdate(getRemoveOperation(), client);
    }

    public static Builder builder(final String name) {
        return new Builder(name);
    }

    public static class PrincipalQueryBuilder {

        private final Builder builder;
        private final String datasource;
        private final String sql;

        private Map<String, ModelNode> passwordMappers = new LinkedHashMap<>();
        private List<ModelNode> attributeMappers = new ArrayList<>();

        PrincipalQueryBuilder(final Builder builder, final String datasource, final String sql) {
            this.builder = builder;
            this.datasource = datasource;
            this.sql = sql;
        }

        public PrincipalQueryBuilder withPasswordMapper(final String passwordType, final String algorithm, final int passwordIndex, final int saltIndex, final int iteractionCountIndex) {
            return withPasswordMapper(passwordType, algorithm, passwordIndex, Encoding.BASE64, saltIndex, Encoding.BASE64, iteractionCountIndex);
        }

        public PrincipalQueryBuilder withPasswordMapper(final String passwordType, final String algorithm, final int passwordIndex, final Encoding passwordEncoding,
                final int saltIndex, final Encoding saltEncoding, final int iteractionCountIndex) {
            ModelNode passwordMapper = new ModelNode();
            if (algorithm != null) {
                passwordMapper.get("algorithm").set(algorithm);
            }
            passwordMapper.get("password-index").set(passwordIndex);
            if (passwordEncoding == Encoding.HEX) {
                passwordMapper.get("hash-encoding").set("hex");
            }

            if (saltIndex > 0) {
                passwordMapper.get("salt-index").set(saltIndex);
                if (saltEncoding == Encoding.HEX) {
                    passwordMapper.get("salt-encoding").set("hex");
                }
            }

            if (iteractionCountIndex > 0) {
                passwordMapper.get("iteration-count-index").set(iteractionCountIndex);
            }
            passwordMappers.put(passwordType, passwordMapper);

            return this;
        }

        public PrincipalQueryBuilder withAttributeMapper(final String attributeName, final int attributeIndex) {
            ModelNode attributeMapper = new ModelNode();
            attributeMapper.get("index").set(attributeIndex);
            attributeMapper.get("to").set(attributeName);
            attributeMappers.add(attributeMapper);

            return this;
        }


        public Builder build() {
            ModelNode principalQuery = new ModelNode();
            principalQuery.get("data-source").set(datasource);
            principalQuery.get("sql").set(sql);
            for (Entry<String, ModelNode> mapper : passwordMappers.entrySet()) {
                principalQuery.get(mapper.getKey()).set(mapper.getValue());
            }
            if (attributeMappers.size() > 0) {
                principalQuery.get("attribute-mapping").set(attributeMappers);
            }

            return builder.addPrincipalQuery(principalQuery);
        }

    }

    public static class Builder {

        private final String name;
        private List<ModelNode> queries = new ArrayList<>();
        private String hashCharset;

        Builder(final String name) {
            this.name = name;
        }

        public PrincipalQueryBuilder withPrincipalQuery(final String datasource, final String sql) {
            return new PrincipalQueryBuilder(this, datasource, sql);
        }

        Builder addPrincipalQuery(final ModelNode principalQuery) {
            queries.add(principalQuery);

            return this;
        }

        public Builder withHashCharset(String hashCharset) {
            this.hashCharset = hashCharset;

            return this;
        }

        public SecurityRealm build() {
            return new JdbcSecurityRealm(name, queries, hashCharset != null ? hashCharset : "UTF-8");
        }

    }

    public enum Encoding {
        BASE64, HEX;
    }



}
