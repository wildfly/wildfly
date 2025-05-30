/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jca.workmanager;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import jakarta.annotation.Resource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.connector.services.workmanager.NamedWorkManager;
import org.jboss.as.connector.services.workmanager.StatisticsExecutorImpl;
import org.jboss.as.test.integration.jca.JcaMgmtBase;
import org.jboss.as.test.integration.jca.JcaMgmtServerSetupTask;
import org.jboss.as.test.integration.jca.JcaTestsUtil;
import org.jboss.as.test.integration.jca.rar.MultipleAdminObject1;
import org.jboss.as.test.integration.jca.rar.MultipleAdminObject1Impl;
import org.jboss.as.test.integration.jca.rar.MultipleConnectionFactory1;
import org.jboss.as.test.integration.jca.rar.MultipleResourceAdapter3;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.shared.PermissionUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.lang.reflect.ReflectPermission;

/**
 * JBQA-6454 Test case: long running threads pool and short running threads pool
 * should be different in case of using work manager with defined long-running-threads property
 * should be equal elsewhere
 *
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(LongRunningThreadsCheckTestCase.TestCaseSetup.class)
public class LongRunningThreadsCheckTestCase {

    public static String ctx = "customContext";
    public static String wm = "customWM";

    static class TestCaseSetup extends JcaMgmtServerSetupTask {
        private boolean enabled;
        private boolean failingOnError;
        private boolean failingOnWarning;

        @Override
        public void doSetup(final ManagementClient managementClient) throws Exception {
            enabled = getArchiveValidationAttribute("enabled");
            failingOnError = getArchiveValidationAttribute("fail-on-error");
            failingOnWarning = getArchiveValidationAttribute("fail-on-warn");
            setArchiveValidation(false, false, false);
            for (int i = 1; i <= 2; i++) {
                ModelNode wmAddress = subsystemAddress.clone().add("workmanager", wm + i);
                ModelNode bsAddress = subsystemAddress.clone().add("bootstrap-context", ctx + i);

                ModelNode operation = new ModelNode();

                try {

                    operation.get(OP).set(ADD);
                    operation.get(OP_ADDR).set(wmAddress);
                    operation.get(NAME).set(wm + i);
                    executeOperation(operation);

                    operation = new ModelNode();
                    operation.get(OP).set(ADD);
                    operation.get(OP_ADDR).set(wmAddress.clone().add("short-running-threads", wm + i));
                    operation.get("core-threads").set("20");
                    operation.get("queue-length").set("20");
                    operation.get("max-threads").set("20");
                    executeOperation(operation);

                    if (i == 2) {
                        operation = new ModelNode();
                        operation.get(OP).set(ADD);
                        operation.get(OP_ADDR).set(wmAddress.clone().add("long-running-threads", wm + i));
                        operation.get("core-threads").set("20");
                        operation.get("queue-length").set("20");
                        operation.get("max-threads").set("20");
                        executeOperation(operation);
                    }

                    operation = new ModelNode();
                    operation.get(OP).set(ADD);
                    operation.get(OP_ADDR).set(bsAddress);
                    operation.get(NAME).set(ctx + i);
                    operation.get("workmanager").set(wm + i);
                    executeOperation(operation);

                } catch (Exception e) {
                    throw new Exception(e.getMessage() + operation, e);
                }
            }
        }
    }

    /**
     * Define the deployment
     *
     * @return The deployment archive
     */
    @Deployment
    public static EnterpriseArchive createDeployment() {

        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "wm.jar");
        ja.addPackage(MultipleConnectionFactory1.class.getPackage()).addClasses(LongRunningThreadsCheckTestCase.class,
                JcaMgmtServerSetupTask.class, JcaMgmtBase.class, JcaTestsUtil.class);
        ja.addPackage(AbstractMgmtTestBase.class.getPackage());
        ja.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.connector, org.jboss.ironjacamar.api, org.jboss.threads"),
                "MANIFEST.MF");

        ResourceAdapterArchive ra1 = ShrinkWrap.create(ResourceAdapterArchive.class, "wm1.rar");
        ra1.addAsManifestResource(LongRunningThreadsCheckTestCase.class.getPackage(), "ra.xml", "ra.xml")
                .addAsManifestResource(LongRunningThreadsCheckTestCase.class.getPackage(), "ironjacamar1.xml",
                        "ironjacamar.xml");

        ResourceAdapterArchive ra2 = ShrinkWrap.create(ResourceAdapterArchive.class, "wm2.rar");
        ra2.addAsManifestResource(LongRunningThreadsCheckTestCase.class.getPackage(), "ra.xml", "ra.xml")
                .addAsManifestResource(LongRunningThreadsCheckTestCase.class.getPackage(), "ironjacamar2.xml",
                        "ironjacamar.xml");

        EnterpriseArchive ea = ShrinkWrap.create(EnterpriseArchive.class, "wm.ear");
        ea.addAsLibrary(ja).addAsModule(ra1).addAsModule(ra2);
        ea.addAsManifestResource(PermissionUtils.createPermissionsXmlAsset(
                new RuntimePermission("accessDeclaredMembers"),
                new ReflectPermission("suppressAccessChecks")
        ), "permissions.xml");

        return ea;
    }

    @Resource(mappedName = "java:jboss/A1")
    private MultipleAdminObject1 adminObject1;

    @Resource(mappedName = "java:jboss/A2")
    private MultipleAdminObject1 adminObject2;

    /**
     * Tests work manager without long-running-threads set
     *
     * @throws Throwable Thrown if case of an error
     */
    @Test
    public void testWmWithoutLongRunningThreads() throws Throwable {

        assertNotNull("A1 not found", adminObject1);

        MultipleAdminObject1Impl impl = (MultipleAdminObject1Impl) adminObject1;
        MultipleResourceAdapter3 adapter = (MultipleResourceAdapter3) impl.getResourceAdapter();
        assertNotNull(adapter);
        NamedWorkManager manager = adapter.getWorkManager();
        assertEquals(wm + 1, manager.getName());
        assertTrue(manager.getShortRunningThreadPool() instanceof StatisticsExecutorImpl);
        assertTrue(manager.getLongRunningThreadPool() instanceof StatisticsExecutorImpl);
        assertEquals(JcaTestsUtil.extractRealExecutor((StatisticsExecutorImpl) manager.getShortRunningThreadPool()),
                JcaTestsUtil.extractRealExecutor((StatisticsExecutorImpl) manager.getLongRunningThreadPool()));
    }

    /**
     * Tests work manager with long-running-threads set
     *
     * @throws Throwable Thrown if case of an error
     */
    @Test
    public void testWmWithLongRunningThreads() throws Throwable {
        assertNotNull("A2 not found", adminObject2);

        MultipleAdminObject1Impl impl = (MultipleAdminObject1Impl) adminObject2;
        MultipleResourceAdapter3 adapter = (MultipleResourceAdapter3) impl.getResourceAdapter();
        assertNotNull(adapter);
        NamedWorkManager manager = adapter.getWorkManager();
        assertEquals(wm + 2, manager.getName());
        assertFalse(manager.getShortRunningThreadPool().equals(manager.getLongRunningThreadPool()));
    }

}
