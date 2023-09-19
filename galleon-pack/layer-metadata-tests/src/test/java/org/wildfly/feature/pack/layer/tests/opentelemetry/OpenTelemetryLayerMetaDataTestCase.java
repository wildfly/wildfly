package org.wildfly.feature.pack.layer.tests.opentelemetry;

import org.junit.Test;
import org.wildfly.feature.pack.layer.tests.AbstractLayerMetaDataTestCase;

import java.nio.file.Path;

public class OpenTelemetryLayerMetaDataTestCase extends AbstractLayerMetaDataTestCase {

    @Test
    public void testAnnotationInInstrumentationAnnotationsPackage() throws Exception {
        testSingleClassWar(OpenTelemetryAnnotationInInstrumentationAnnotationsPackageUsage.class);
    }

    @Test
    public void testClassInApiBaggagePackage() throws Exception {
        testSingleClassWar(OpenTelemetryClassInApiBaggagePackageUsage.class);
    }

    @Test
    public void testClassInApiBaggagePropagationPackage() throws Exception {
        testSingleClassWar(OpenTelemetryClassInApiBaggagePropagationPackageUsage.class);
    }

    @Test
    public void testClassInApiCommonPackage() throws Exception {
        testSingleClassWar(OpenTelemetryClassInApiCommonPackageUsage.class);
    }

    @Test
    public void testClassInApiMetricsPackage() throws Exception {
        testSingleClassWar(OpenTelemetryClassInApiMetricsPackageUsage.class);
    }

    @Test
    public void testClassInApiTracePackage() throws Exception {
        testSingleClassWar(OpenTelemetryClassInApiTracePackageUsage.class);
    }

    @Test
    public void testClassInApiTracePropagationPackage() throws Exception {
        testSingleClassWar(OpenTelemetryClassInApiTracePropagationPackageUsage.class);
    }

    @Test
    public void testClassInApiLogsPackage() throws Exception {
        testSingleClassWar(OpenTelemetryClassInApiLogsPackageUsage.class);
    }

    @Test
    public void testClassInSdkPackage() throws Exception {
        testSingleClassWar(OpenTelemetryClassInSdkPackageUsage.class);
    }

    @Test
    public void testClassInSdkCommonPackage() throws Exception {
        testSingleClassWar(OpenTelemetryClassInSdkCommonPackageUsage.class);
    }

    @Test
    public void testClassInSdkAutoConfigurePackage() throws Exception {
        testSingleClassWar(OpenTelemetryClassInSdkAutoConfigurePackageUsage.class);
    }

    @Test
    public void testClassInSdkAutoConfigureSpiPackage() throws Exception {
        testSingleClassWar(OpenTelemetryClassInSdkAutoConfigureSpiPackageUsage.class);
    }

    @Test
    public void testClassInSdkAutoConfigureSpiLogsPackage() throws Exception {
        testSingleClassWar(OpenTelemetryClassInSdkAutoConfigureSpiLogsPackageUsage.class);
    }

    @Test
    public void testClassInSdkAutoConfigureSpiMetricsPackage() throws Exception {
        testSingleClassWar(OpenTelemetryClassInSdkAutoConfigureSpiMetricsPackageUsage.class);
    }

    @Test
    public void testClassInSdkAutoConfigureSpiTracesPackage() throws Exception {
        testSingleClassWar(OpenTelemetryClassInSdkAutoConfigureSpiTracesPackageUsage.class);
    }

    @Test
    public void testClassInSdkLogsPackage() throws Exception {
        testSingleClassWar(OpenTelemetryClassInSdkLogsPackageUsage.class);
    }

    @Test
    public void testClassInSdkLogsDataPackage() throws Exception {
        testSingleClassWar(OpenTelemetryClassInSdkLogsDataPackageUsage.class);
    }

    @Test
    public void testClassInSdkLogsExportPackage() throws Exception {
        testSingleClassWar(OpenTelemetryClassInSdkLogsExportPackageUsage.class);
    }

    @Test
    public void testClassInSdkMetricsPackage() throws Exception {
        testSingleClassWar(OpenTelemetryClassInSdkMetricsPackageUsage.class);
    }

    @Test
    public void testClassInSdkMetricsDataPackage() throws Exception {
        testSingleClassWar(OpenTelemetryClassInSdkMetricsDataPackageUsage.class);
    }

    @Test
    public void testClassInSdkMetricsExportPackage() throws Exception {
        testSingleClassWar(OpenTelemetryClassInSdkMetricsExportPackageUsage.class);
    }

    @Test
    public void testClassInSdkTracePackage() throws Exception {
        testSingleClassWar(OpenTelemetryClassInSdkTracePackageUsage.class);
    }

    @Test
    public void testClassInSdkTraceDataPackage() throws Exception {
        testSingleClassWar(OpenTelemetryClassInSdkTraceDataPackageUsage.class);
    }

    @Test
    public void testClassInSdkTraceExportPackage() throws Exception {
        testSingleClassWar(OpenTelemetryClassInSdkTraceExportPackageUsage.class);
    }

    @Test
    public void testClassInSdkTraceSamplersPackage() throws Exception {
        testSingleClassWar(OpenTelemetryClassInSdkTraceSamplersPackageUsage.class);
    }

    private void testSingleClassWar(Class<?> clazz) throws Exception {
        Path p = createArchiveBuilder(ArchiveType.WAR)
                .addClasses(clazz)
                .build();
        checkLayersForArchive(p, "opentelemetry");
    }
}
