/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.batch.common;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.PropertyPermission;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.batch.runtime.JobExecution;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.Asset;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptors;
import org.jboss.shrinkwrap.descriptor.api.spec.se.manifest.ManifestDescriptor;
import org.junit.Assert;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public abstract class AbstractBatchTestCase {
    static final String ENCODING = "utf-8";

    public static WebArchive createDefaultWar(final String warName, final Package pkg, final String... jobXmls) {
        final WebArchive deployment = ShrinkWrap.create(WebArchive.class, warName)
                .addPackage(AbstractBatchTestCase.class.getPackage())
                .addClasses(TimeoutUtil.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .setManifest(new StringAsset(
                        Descriptors.create(ManifestDescriptor.class)
                                .attribute("Dependencies", "org.jboss.msc,org.wildfly.security.manager")
                                .exportAsString()))
                .addAsManifestResource(createPermissionsXmlAsset(new PropertyPermission("ts.timeout.factor", "read")), "permissions.xml");
        for (String jobXml : jobXmls) {
            addJobXml(pkg, deployment, jobXml);
        }
        return deployment;
    }

    protected static String performCall(final String url) throws ExecutionException, IOException, TimeoutException {
        return performCall(url, 10);
    }

    protected static String performCall(final String url, final int timeout) throws ExecutionException, IOException, TimeoutException {
        return HttpRequest.get(url, TimeoutUtil.adjust(timeout), TimeUnit.SECONDS);
    }

    protected static WebArchive addJobXml(final Package pkg, final WebArchive deployment, final String jobXml) {
        return addJobXml(pkg, deployment, jobXml, jobXml);
    }

    protected static WebArchive addJobXml(final Package pkg, final WebArchive deployment, final String fileName, final String jobXml) {
        return deployment.addAsWebInfResource(pkg, fileName, "classes/META-INF/batch-jobs/" + jobXml);
    }

    protected static WebArchive addJobXml(final WebArchive deployment, final Asset asset, final String jobXml) {
        return deployment.addAsWebInfResource(asset, "classes/META-INF/batch-jobs/" + jobXml);
    }

    protected static WebArchive addJobXml(final WebArchive deployment, final String jobName) {
        return deployment.addAsWebInfResource(createJobXml(jobName), "classes/META-INF/batch-jobs/" + jobName + ".xml");
    }

    protected static JavaArchive addJobXml(final JavaArchive deployment, final String jobName) {
        return deployment.addAsResource(createJobXml(jobName), "META-INF/batch-jobs/" + jobName + ".xml");
    }

    static Asset createJobXml(final String jobName) {
        final String xml = "<job id=\"" + jobName + "\" xmlns=\"http://xmlns.jcp.org/xml/ns/javaee\" version=\"1.0\">\n" +
                "    <step id=\"step1\">\n" +
                "        <chunk item-count=\"3\">\n" +
                "            <reader ref=\"countingItemReader\">\n" +
                "                <properties>\n" +
                "                    <property name=\"reader.start\" value=\"#{jobParameters['reader.start']}\"/>\n" +
                "                    <property name=\"reader.end\" value=\"#{jobParameters['reader.end']}\"/>\n" +
                "                </properties>\n" +
                "            </reader>\n" +
                "            <writer ref=\"countingItemWriter\">\n" +
                "                <properties>\n" +
                "                    <property name=\"writer.sleep.time\" value=\"#{jobParameters['writer.sleep.time']}\"/>\n" +
                "                </properties>\n" +
                "            </writer>\n" +
                "        </chunk>\n" +
                "    </step>\n" +
                "</job>";
        return new StringAsset(xml);
    }

    protected static void waitForTermination(final JobExecution jobExecution, final int timeout) {
        long waitTimeout = TimeoutUtil.adjust(timeout * 1000);
        long sleep = 100L;
        while (true) {
            switch (jobExecution.getBatchStatus()) {
                case STARTED:
                case STARTING:
                case STOPPING:
                    try {
                        TimeUnit.MILLISECONDS.sleep(sleep);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    waitTimeout -= sleep;
                    sleep = Math.max(sleep / 2, 100L);
                    break;
                default:
                    return;
            }
            if (waitTimeout <= 0) {
                throw new IllegalStateException("Batch job did not complete within allotted time.");
            }
        }
    }

    public static class UrlBuilder {
        private final URL url;
        private final String[] paths;
        private final Map<String, String> params;

        private UrlBuilder(final URL url, final String... paths) {
            this.url = url;
            this.paths = paths;
            params = new HashMap<String, String>();
        }

        public static UrlBuilder of(final URL url, final String... paths) {
            return new UrlBuilder(url, paths);
        }

        public UrlBuilder addParameter(final String key, final int value) {
            return addParameter(key, Integer.toString(value));
        }

        public UrlBuilder addParameter(final String key, final String value) {
            params.put(key, value);
            return this;
        }

        public String build() throws UnsupportedEncodingException {
            final StringBuilder result = new StringBuilder(url.toExternalForm());
            if (paths != null) {
                for (String path : paths) {
                    result.append('/').append(path);
                }
            }
            boolean isFirst = true;
            for (String key : params.keySet()) {
                if (isFirst) {
                    result.append('?');
                } else {
                    result.append('&');
                }
                final String value = params.get(key);
                result.append(URLEncoder.encode(key, ENCODING)).append('=').append(URLEncoder.encode(value, ENCODING));
                isFirst = false;
            }
            return result.toString();
        }
    }

    public static class LoggingSetup implements ServerSetupTask {

        static final ModelNode WELD_BOOTSTRAP_LOGGER_ADDRESS;

        static {
            WELD_BOOTSTRAP_LOGGER_ADDRESS = new ModelNode().setEmptyList();
            WELD_BOOTSTRAP_LOGGER_ADDRESS.add(ModelDescriptionConstants.SUBSYSTEM, "logging");
            WELD_BOOTSTRAP_LOGGER_ADDRESS.add("logger", "org.jboss.weld.Bootstrap");
            WELD_BOOTSTRAP_LOGGER_ADDRESS.protect();
        }

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {

            final ModelControllerClient client = managementClient.getControllerClient();

            // Create the weld bootstrap logger
            ModelNode op = Operations.createAddOperation(WELD_BOOTSTRAP_LOGGER_ADDRESS);
            op.get("level").set("DEBUG");
            execute(client, op);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            final ModelControllerClient client = managementClient.getControllerClient();

            // Remove the weld bootstrap logger
            ModelNode op = Operations.createRemoveOperation(WELD_BOOTSTRAP_LOGGER_ADDRESS);
            execute(client, op);
        }

        static ModelNode execute(final ModelControllerClient client, final ModelNode op) throws IOException {
            ModelNode result = client.execute(op);
            if (!Operations.isSuccessfulOutcome(result)) {
                Assert.assertTrue(Operations.getFailureDescription(result).toString(), false);
            }
            return result;
        }
    }
}
