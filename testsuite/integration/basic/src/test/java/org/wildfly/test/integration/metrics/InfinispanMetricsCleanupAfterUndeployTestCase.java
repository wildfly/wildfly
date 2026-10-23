/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.integration.metrics;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FILE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FILE_HANDLER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LOGGER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.awaitility.Awaitility.await;
import static org.jboss.as.test.shared.ServerReload.executeReloadAndWaitForCompletion;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.wildfly.test.integration.metrics.MetricsHelper.getPrometheusMetrics;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.shared.util.AssumeTestGroupUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Verifies that Infinispan session cache metrics are registered when a distributable WAR is deployed
 * post-startup, and that those metrics are cleanly removed on undeploy without ghost reads.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(InfinispanMetricsCleanupAfterUndeployTestCase.SetupTask.class)
public class InfinispanMetricsCleanupAfterUndeployTestCase {

    static final String DEPLOYMENT_NAME = "InfinispanMetricsCleanup";
    static final String WAR_NAME = DEPLOYMENT_NAME + ".war";
    static final String LOG_FILE_NAME = "infinispan-metrics-debug.log";
    static final String METRICS_LOGGER_HANDLER = "infinispan-metrics-test-handler";
    static final String METRICS_LOGGER_CATEGORY = "org.wildfly.extension.metrics";
    static final String METRICS_PRIVATE_LOGGER_CATEGORY = "org.wildfly.extension.metrics._private";

    static final ModelNode FILE_HANDLER_ADDR = PathAddress.pathAddress(SUBSYSTEM, "logging")
            .append(FILE_HANDLER, METRICS_LOGGER_HANDLER).toModelNode();
    static final ModelNode LOGGER_ADDR = PathAddress.pathAddress(SUBSYSTEM, "logging")
            .append(LOGGER, METRICS_LOGGER_CATEGORY).toModelNode();
    static final ModelNode PRIVATE_LOGGER_ADDR = PathAddress.pathAddress(SUBSYSTEM, "logging")
            .append(LOGGER, METRICS_PRIVATE_LOGGER_CATEGORY).toModelNode();
    static final ModelNode INFINISPAN_WEB_ADDR = PathAddress.pathAddress(SUBSYSTEM, "infinispan")
            .append("cache-container", "web").toModelNode();
    static final ModelNode INFINISPAN_PASSIVATION_ADDR = PathAddress.pathAddress(SUBSYSTEM, "infinispan")
            .append("cache-container", "web").append("local-cache", "passivation").toModelNode();

    static String serverLogDir;

    static class SetupTask implements ServerSetupTask {

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
                ModelNode enableStats = new ModelNode();
            enableStats.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            enableStats.get(OP_ADDR).set(INFINISPAN_WEB_ADDR);
            enableStats.get(NAME).set("statistics-enabled");
            enableStats.get(VALUE).set(true);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), enableStats);

            // Session caches are cloned from the passivation template at deployment time; the
            // template must have statistics enabled so that CacheMgmtInterceptor is active and
            // metrics have values. A reload is required to propagate the change before deployment.
            ModelNode enableCacheStats = new ModelNode();
            enableCacheStats.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            enableCacheStats.get(OP_ADDR).set(INFINISPAN_PASSIVATION_ADDR);
            enableCacheStats.get(NAME).set("statistics-enabled");
            enableCacheStats.get(VALUE).set(true);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), enableCacheStats);
            executeReloadAndWaitForCompletion(managementClient);

            ModelNode fileHandler = new ModelNode();
            fileHandler.get(OP).set(ADD);
            fileHandler.get(OP_ADDR).set(FILE_HANDLER_ADDR);
            fileHandler.get("level").set("DEBUG");
            fileHandler.get("append").set(true);
            fileHandler.get("autoflush").set(true);
            ModelNode file = new ModelNode();
            file.get("relative-to").set("jboss.server.log.dir");
            file.get("path").set(LOG_FILE_NAME);
            fileHandler.get(FILE).set(file);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), fileHandler);

            ModelNode logger = new ModelNode();
            logger.get(OP).set(ADD);
            logger.get(OP_ADDR).set(LOGGER_ADDR);
            logger.get("level").set("DEBUG");
            logger.get("use-parent-handlers").set(false);
            ModelNode handlers = new ModelNode();
            handlers.add(METRICS_LOGGER_HANDLER);
            logger.get("handlers").set(handlers);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), logger);

            ModelNode privateLogger = new ModelNode();
            privateLogger.get(OP).set(ADD);
            privateLogger.get(OP_ADDR).set(PRIVATE_LOGGER_ADDR);
            privateLogger.get("level").set("DEBUG");
            privateLogger.get("use-parent-handlers").set(false);
            ModelNode privateHandlers = new ModelNode();
            privateHandlers.add(METRICS_LOGGER_HANDLER);
            privateLogger.get("handlers").set(privateHandlers);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), privateLogger);

            ModelNode resolveExpr = new ModelNode();
            resolveExpr.get(OP).set("resolve-expression");
            resolveExpr.get("expression").set("${jboss.server.log.dir}");
            serverLogDir = ManagementOperations
                    .executeOperation(managementClient.getControllerClient(), resolveExpr).asString();
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            ModelNode removePrivateLogger = new ModelNode();
            removePrivateLogger.get(OP).set(REMOVE);
            removePrivateLogger.get(OP_ADDR).set(PRIVATE_LOGGER_ADDR);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), removePrivateLogger);

            ModelNode removeLogger = new ModelNode();
            removeLogger.get(OP).set(REMOVE);
            removeLogger.get(OP_ADDR).set(LOGGER_ADDR);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), removeLogger);

            ModelNode removeHandler = new ModelNode();
            removeHandler.get(OP).set(REMOVE);
            removeHandler.get(OP_ADDR).set(FILE_HANDLER_ADDR);
            ManagementOperations.executeOperation(managementClient.getControllerClient(), removeHandler);

            ModelNode undefineCacheStats = new ModelNode();
            undefineCacheStats.get(OP).set("undefine-attribute");
            undefineCacheStats.get(OP_ADDR).set(INFINISPAN_PASSIVATION_ADDR);
            undefineCacheStats.get(NAME).set("statistics-enabled");
            ManagementOperations.executeOperation(managementClient.getControllerClient(), undefineCacheStats);

            ModelNode undefineStats = new ModelNode();
            undefineStats.get(OP).set("undefine-attribute");
            undefineStats.get(OP_ADDR).set(INFINISPAN_WEB_ADDR);
            undefineStats.get(NAME).set("statistics-enabled");
            ManagementOperations.executeOperation(managementClient.getControllerClient(), undefineStats);

            ModelNode checkDeployment = new ModelNode();
            checkDeployment.get(OP).set(READ_RESOURCE_OPERATION);
            checkDeployment.get(OP_ADDR).set(PathAddress.pathAddress("deployment", WAR_NAME).toModelNode());
            ModelNode checkResult = managementClient.getControllerClient().execute(checkDeployment);
            if (!checkResult.hasDefined(FAILURE_DESCRIPTION)) {
                ModelNode removeDeployment = new ModelNode();
                removeDeployment.get(OP).set(REMOVE);
                removeDeployment.get(OP_ADDR).set(PathAddress.pathAddress("deployment", WAR_NAME).toModelNode());
                managementClient.getControllerClient().execute(removeDeployment);
            }

            new File(serverLogDir, LOG_FILE_NAME).delete();
        }
    }

    @Deployment(name = DEPLOYMENT_NAME, managed = false, testable = false)
    public static WebArchive deploy() {
        String webXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<web-app xmlns=\"https://jakarta.ee/xml/ns/jakartaee\"\n"
                + "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "         xsi:schemaLocation=\"https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd\"\n"
                + "         version=\"6.0\">\n"
                + "    <distributable/>\n"
                + "</web-app>\n";
        return ShrinkWrap.create(WebArchive.class, WAR_NAME)
                .addAsWebInfResource(new StringAsset(webXml), "web.xml");
    }

    @BeforeClass
    public static void skipSecurityManager() {
        AssumeTestGroupUtil.assumeSecurityManagerDisabled();
    }

    @BeforeClass
    public static void skipPreview() {
        AssumeTestGroupUtil.assumeNotWildFlyPreview();
    }

    @ContainerResource
    ManagementClient managementClient;

    @ArquillianResource
    Deployer deployer;

    @Test
    @InSequence(1)
    public void testDeployAndVerifyInfinispanCacheMetricsNotExposed() throws Exception {
        deployer.deploy(DEPLOYMENT_NAME);

        assertTrue("Infinispan cache DMR resource must exist after deploy",
                SUCCESS.equals(dmrRead(infinispanDeploymentCacheAddress()).get(OUTCOME).asString()));

        // Metric registration is async (ResourceMetricsRetryQueue) — await until it appears
        await("Infinispan cache metrics must appear in Prometheus after deploy")
                .atMost(30, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> getPrometheusMetrics(managementClient, true).contains("cache=\"" + WAR_NAME + "\""));
    }

    @Test
    @InSequence(2)
    public void testRestartUndeployAndVerifyNoGhostMetricReads() throws Exception {
        // Reload so the WAR participates in the boot-time global scan, registering WildFlyMetric
        // entries that are NOT cleaned up by the notification path (the original ghost-read bug).
        executeReloadAndWaitForCompletion(managementClient);

        assertTrue("Infinispan cache DMR resource must exist after reload",
                SUCCESS.equals(dmrRead(infinispanDeploymentCacheAddress()).get(OUTCOME).asString()));

        deployer.undeploy(DEPLOYMENT_NAME);

        assertFalse("Infinispan cache DMR resource must be gone after undeploy",
                SUCCESS.equals(dmrRead(infinispanDeploymentCacheAddress()).get(OUTCOME).asString()));

        String metricsAfterUndeploy = getPrometheusMetrics(managementClient, true);
        assertFalse("No undertow metrics must remain after undeploy", metricsAfterUndeploy.contains("deployment=\"" + WAR_NAME + "\""));
        assertFalse("No Infinispan cache metrics must remain after undeploy", metricsAfterUndeploy.contains("cache=\"" + WAR_NAME + "\""));

        // Scrape /metrics twice more to trigger any ghost reads, then let the log flush.
        getPrometheusMetrics(managementClient, true);
        getPrometheusMetrics(managementClient, true);
        Thread.sleep(500);

        Path logPath = Paths.get(serverLogDir, LOG_FILE_NAME);
        assertTrue("Debug log file must exist — logger setup broken if missing: " + logPath, Files.exists(logPath));
        List<String> logLines = Files.readAllLines(logPath);

        // WFLYCTL0216 spans multiple lines: "Unable to read attribute" + WAR_NAME in the address.
        boolean hasGhostRead = logLines.stream().anyMatch(l -> l.contains("Unable to read attribute"));
        boolean hasDeploymentRef = logLines.stream().anyMatch(l -> l.contains(WAR_NAME));
        assertFalse("Ghost metric reads detected after undeploy (WFLY-22353)", hasGhostRead && hasDeploymentRef);
    }

    private ModelNode dmrRead(ModelNode address) throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set(READ_RESOURCE_OPERATION);
        op.get(OP_ADDR).set(address);
        return managementClient.getControllerClient().execute(op);
    }

    private ModelNode infinispanDeploymentCacheAddress() {
        return PathAddress.pathAddress(
                PathElement.pathElement(SUBSYSTEM, "infinispan"),
                PathElement.pathElement("cache-container", "web"),
                PathElement.pathElement("cache", WAR_NAME)
        ).toModelNode();
    }
}
