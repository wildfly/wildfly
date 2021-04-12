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


package org.wildfly.test.integration.microprofile.metrics.application;

import static org.wildfly.test.integration.microprofile.metrics.MetricsHelper.getJSONMetrics;
import static org.wildfly.test.integration.microprofile.metrics.MetricsHelper.getMetricSubValueFromJSONOutput;
import static org.wildfly.test.integration.microprofile.metrics.MetricsHelper.getMetricValueFromPrometheusOutput;
import static org.wildfly.test.integration.microprofile.metrics.MetricsHelper.getPrometheusMetrics;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.metrics.TestApplication;
import org.wildfly.test.integration.microprofile.metrics.application.resource.ResourceMeteredTimed;


/**
 * Regression tests for SmallRye Metrics issues:
 * https://github.com/smallrye/smallrye-metrics/issues/39
 *   Difference between Prometheus and JSON format - Metered annotation
 *
 * https://github.com/smallrye/smallrye-metrics/issues/40
 *   Difference between Prometheus and JSON format - Timed annotation
 */
@RunWith(Arquillian.class)
@RunAsClient
public class MicroProfileMetricsDifferentFormatsValueTestCase {

    /**
     * Prometheus metrics
     */
    private String prometheus;

    /**
     * Json metrics
     */
    private String json;

    /**
     * Prepare deployment
     */
    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, MicroProfileMetricsDifferentFormatsValueTestCase.class.getSimpleName() + ".war")
                .addClasses(TestApplication.class)
                .addClass(ResourceMeteredTimed.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    /**
     * https://issues.jboss.org/browse/WFLY-11499
     */
    @BeforeClass
    public static void skipSecurityManager() {
        AssumeTestGroupUtil.assumeSecurityManagerDisabled();
    }

    /**
     * ManagementClient provides management port and hostname
     */
    @ContainerResource
    ManagementClient managementClient;

    /**
     * Check Metered annotation
     */
    @Test
    public void testMetered(@ArquillianResource URL url) throws Exception {
        // requests
        performCallsForOneEndPoint(url, "metered");

        // asserts
        commonAsserts("metered");
        diffAssert("metered", "count", 1, "metered_total", 0.0);
    }

    /**
     * Check Timed annotation
     */
    @Test
    public void testTimed(@ArquillianResource URL url) throws Exception {
        // requests
        performCallsForOneEndPoint(url, "timed");

        // asserts for same values as metered
        diffAssert("timed", "count", 1, "timed_seconds_count", 0.0);
        commonAsserts("timed");
        // followed values has different units for json and prometheus format. Minutes is unit for json, seconds is unit for prometheus.
        diffAssert("timed", "min", 60, "timed_min_seconds", 1.0E-10 * TimeoutUtil.adjust(1));
        diffAssert("timed", "mean", 60, "timed_mean_seconds", 1.0E-10 * TimeoutUtil.adjust(1));
        diffAssert("timed", "max", 60, "timed_max_seconds", 1.0E-10 * TimeoutUtil.adjust(1));
        diffAssert("timed", "stddev", 60, "timed_stddev_seconds", 1.0E-10 * TimeoutUtil.adjust(1));
        diffAssert("timed", "p50", 60, "timed_seconds{quantile=\"0.5\"}", 1.0E-10 * TimeoutUtil.adjust(1));
        diffAssert("timed", "p75", 60, "timed_seconds{quantile=\"0.75\"}", 1.0E-10 * TimeoutUtil.adjust(1));
        diffAssert("timed", "p95", 60, "timed_seconds{quantile=\"0.95\"}", 1.0E-10 * TimeoutUtil.adjust(1));
        diffAssert("timed", "p98", 60, "timed_seconds{quantile=\"0.98\"}", 1.0E-10 * TimeoutUtil.adjust(1));
        diffAssert("timed", "p99", 60, "timed_seconds{quantile=\"0.99\"}", 1.0E-10 * TimeoutUtil.adjust(1));
        diffAssert("timed", "p999", 60, "timed_seconds{quantile=\"0.999\"}", 1.0E-10 * TimeoutUtil.adjust(1));
    }

    /**
     * Common asserts for both timed and metered annotations
     */
    private void commonAsserts(String measuredName) {
        diffAssert(measuredName, "meanRate", 1, measuredName + "_rate_per_second", TimeoutUtil.adjust(15));
        diffAssert(measuredName, "oneMinRate", 1, measuredName + "_one_min_rate_per_second", 0.1 * TimeoutUtil.adjust(1));
        diffAssert(measuredName, "fiveMinRate", 1, measuredName + "_five_min_rate_per_second", 0.1 * TimeoutUtil.adjust(1));
        diffAssert(measuredName, "fifteenMinRate", 1, measuredName + "_fifteen_min_rate_per_second", 0.1 * TimeoutUtil.adjust(1));
    }

    /**
     * Assert maximal diff for json and prometheus values:
     * Assert.true(application:{prometheusName} - ({jsonName}->{jsonSubName} * jsonMultiplier) <= {maximalDiff})
     */
    private void diffAssert(String jsonName, String jsonSubName, int jsonMultiplier, String prometheusName, double maximalDiff) {
        Double diff = getMetricValueFromPrometheusOutput(prometheus, "application", prometheusName)
                - getMetricSubValueFromJSONOutput(json, jsonName, jsonSubName) * jsonMultiplier;
        Assert.assertTrue(String.format("Different %s value in json and prometheus format, difference is %f", jsonName, diff),
                diff <= maximalDiff);
    }

    /**
     * Perform end-point calls and get metrics data
     */
    private void performCallsForOneEndPoint(URL url, String urlSuffix) throws Exception {
        // end-point requests
        for (int i = 0; i < 10; i++) {
            performSingleCall(url, urlSuffix);
        }

        // time average per sec would be different if measurement will be provided right after requests
        // so two metrics requests needs to be done after some delay because
        Thread.sleep(100);

        // measure
        prometheus = getPrometheusMetrics(managementClient, "application/" + urlSuffix, true);
        json = getJSONMetrics(managementClient, "application/" + urlSuffix, true);
    }

    /**
     * Perform a call that should be analyzed by Metrics
     */
    private static String performSingleCall(URL url, String urlSuffix) throws Exception {
        URL appURL = new URL(url.toExternalForm() + "microprofile-metrics-app/" + urlSuffix);
        return HttpRequest.get(appURL.toExternalForm(), 10, TimeUnit.SECONDS);
    }
}
