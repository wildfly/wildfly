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

import java.util.Objects;

import org.jboss.dmr.ModelNode;

/**
 * Representation of name and value attribute pair in domain model.
 *
 * @author Josef Cacek
 */
public class NameValue implements ModelNodeConvertable {

    private final String name;
    private final String value;

    private NameValue(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "Value of 'name' attribute has to be provided.");
        this.value = Objects.requireNonNull(builder.value, "Value of 'value' attribute has to be provided.");
    }

    @Override
    public ModelNode toModelNode() {
        final ModelNode node = new ModelNode();
        node.get("name").set(name);
        node.get("value").set(value);
        return null;
    }

    /**
     * Creates builder to build {@link NameValue}.
     * @return created builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static NameValue from(String name, String value) {
        return builder().withName(name).withValue(value).build();
    }

    /**
     * Builder to build {@link NameValue}.
     */
    public static final class Builder {
        private String name;
        private String value;

        private Builder() {
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withValue(String value) {
            this.value = value;
            return this;
        }

        public NameValue build() {
            return new NameValue(this);
        }
    }
}
