/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.feature.pack.layer.tests.opentelemetry;


import io.opentelemetry.instrumentation.annotations.WithSpan;

public class OpenTelemetryAnnotationInInstrumentationAnnotationsPackageUsage {
    @WithSpan
    public void test() {

    }
}
