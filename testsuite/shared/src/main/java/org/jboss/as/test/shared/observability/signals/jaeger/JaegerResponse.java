/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability.signals.jaeger;

import java.util.List;

@Deprecated
public class JaegerResponse {
    private List<JaegerTrace> data;
    private String errors;

    public List<JaegerTrace> getData() {
        return data;
    }

    public JaegerResponse setData(List<JaegerTrace> data) {
        this.data = data;
        return this;
    }

    public String getErrors() {
        return errors;
    }

    public JaegerResponse setErrors(String errors) {
        this.errors = errors;
        return this;
    }
}
