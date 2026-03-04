/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability.signals.jaeger;

@Deprecated
public class JaegerTag {
    private String key;
    private String type;
    private String value;

    public String getKey() {
        return key;
    }

    public JaegerTag setKey(String key) {
        this.key = key;
        return this;
    }

    public String getType() {
        return type;
    }

    public JaegerTag setType(String type) {
        this.type = type;
        return this;
    }

    public String getValue() {
        return value;
    }

    public JaegerTag setValue(String value) {
        this.value = value;
        return this;
    }

    @Override
    public String toString() {
        return "JaegerTag{" +
                "key='" + key + '\'' +
                ", type='" + type + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
