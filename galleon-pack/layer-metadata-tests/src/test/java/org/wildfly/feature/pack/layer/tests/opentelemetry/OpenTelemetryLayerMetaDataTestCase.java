/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.feature.pack.layer.tests.opentelemetry;

import org.junit.Test;
import org.wildfly.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class OpenTelemetryLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {

    @Test
    public void testAnnotationInInstrumentationAnnotationsPackage() {
        testSingleClassWar(OpenTelemetryAnnotationInInstrumentationAnnotationsPackageUsage.class);
    }

    @Test
    public void testClassInApiBaggagePackage() {
        testSingleClassWar(OpenTelemetryClassInApiBaggagePackageUsage.class);
    }

    @Test
    public void testClassInApiBaggagePropagationPackage() {
        testSingleClassWar(OpenTelemetryClassInApiBaggagePropagationPackageUsage.class);
    }

    @Test
    public void testClassInApiCommonPackage() {
        testSingleClassWar(OpenTelemetryClassInApiCommonPackageUsage.class);
    }

    @Test
    public void testClassInApiMetricsPackage() {
        testSingleClassWar(OpenTelemetryClassInApiMetricsPackageUsage.class);
    }

    @Test
    public void testClassInApiTracePackage() {
        testSingleClassWar(OpenTelemetryClassInApiTracePackageUsage.class);
    }

    @Test
    public void testClassInApiTracePropagationPackage() {
        testSingleClassWar(OpenTelemetryClassInApiTracePropagationPackageUsage.class);
    }

    @Test
    public void testClassInApiLogsPackage() {
        testSingleClassWar(OpenTelemetryClassInApiLogsPackageUsage.class);
    }

    @Test
    public void testClassInSdkPackage() {
        testSingleClassWar(OpenTelemetryClassInSdkPackageUsage.class);
    }

    @Test
    public void testClassInSdkCommonPackage() {
        testSingleClassWar(OpenTelemetryClassInSdkCommonPackageUsage.class);
    }

    @Test
    public void testClassInSdkAutoConfigurePackage() {
        testSingleClassWar(OpenTelemetryClassInSdkAutoConfigurePackageUsage.class);
    }

    @Test
    public void testClassInSdkAutoConfigureSpiPackage() {
        testSingleClassWar(OpenTelemetryClassInSdkAutoConfigureSpiPackageUsage.class);
    }

    @Test
    public void testClassInSdkAutoConfigureSpiLogsPackage() {
        testSingleClassWar(OpenTelemetryClassInSdkAutoConfigureSpiLogsPackageUsage.class);
    }

    @Test
    public void testClassInSdkAutoConfigureSpiMetricsPackage() {
        testSingleClassWar(OpenTelemetryClassInSdkAutoConfigureSpiMetricsPackageUsage.class);
    }

    @Test
    public void testClassInSdkAutoConfigureSpiTracesPackage() {
        testSingleClassWar(OpenTelemetryClassInSdkAutoConfigureSpiTracesPackageUsage.class);
    }

    @Test
    public void testClassInSdkLogsPackage() {
        testSingleClassWar(OpenTelemetryClassInSdkLogsPackageUsage.class);
    }

    @Test
    public void testClassInSdkLogsDataPackage() {
        testSingleClassWar(OpenTelemetryClassInSdkLogsDataPackageUsage.class);
    }

    @Test
    public void testClassInSdkLogsExportPackage() {
        testSingleClassWar(OpenTelemetryClassInSdkLogsExportPackageUsage.class);
    }

    @Test
    public void testClassInSdkMetricsPackage() {
        testSingleClassWar(OpenTelemetryClassInSdkMetricsPackageUsage.class);
    }

    @Test
    public void testClassInSdkMetricsDataPackage() {
        testSingleClassWar(OpenTelemetryClassInSdkMetricsDataPackageUsage.class);
    }

    @Test
    public void testClassInSdkMetricsExportPackage() {
        testSingleClassWar(OpenTelemetryClassInSdkMetricsExportPackageUsage.class);
    }

    @Test
    public void testClassInSdkTracePackage() {
        testSingleClassWar(OpenTelemetryClassInSdkTracePackageUsage.class);
    }

    @Test
    public void testClassInSdkTraceDataPackage() {
        testSingleClassWar(OpenTelemetryClassInSdkTraceDataPackageUsage.class);
    }

    @Test
    public void testClassInSdkTraceExportPackage() {
        testSingleClassWar(OpenTelemetryClassInSdkTraceExportPackageUsage.class);
    }

    @Test
    public void testClassInSdkTraceSamplersPackage() {
        testSingleClassWar(OpenTelemetryClassInSdkTraceSamplersPackageUsage.class);
    }

    private void testSingleClassWar(Class<?> clazz) {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(clazz)
                .build();
        checkLayersForArchive(p, new ExpectedLayers("opentelemetry", "opentelemetry"));
    }
}
