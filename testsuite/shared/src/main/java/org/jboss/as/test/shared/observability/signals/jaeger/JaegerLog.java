/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability.signals.jaeger;

import java.util.List;

@Deprecated
public class JaegerLog {
    private Long timestamp;
    private List<JaegerTag> fields;

    public Long getTimestamp() {
        return timestamp;
    }

    public JaegerLog setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public List<JaegerTag> getFields() {
        return fields;
    }

    public JaegerLog setFields(List<JaegerTag> fields) {
        this.fields = fields;
        return this;
    }

    @Override
    public String toString() {
        return "JaegerLog{" +
                "timestamp=" + timestamp +
                ", fields=" + fields +
                '}';
    }
}
