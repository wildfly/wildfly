/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.jca.workmanager;

import javax.annotation.Resource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.connector.services.workmanager.NamedWorkManager;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.jca.JcaMgmtBase;
import org.jboss.as.test.integration.jca.JcaMgmtServerSetupTask;
import org.jboss.as.test.integration.jca.rar.*;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * JBQA-6454 Test case: long running threads pool and short running threads pool
 * should be different in case of using work manager with defined long-running-threads property
 * should be equal elsewhere
 *
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(LongRunningThreadsCheckTestCase.TestCaseSetup.class)
@Ignore("causing an osgi error during server reload")
public class LongRunningThreadsCheckTestCase extends JcaMgmtBase {

    public static String ctx = "customContext";
    public static String wm = "customWM";

    static class TestCaseSetup extends JcaMgmtServerSetupTask {

        @Override
        public void doSetup(final ManagementClient managementClient) throws Exception {

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

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            for (int i = 1; i <= 2; i++) {
                ModelNode wmAddress = subsystemAddress.clone().add("workmanager", wm + i);
                ModelNode bsAddress = subsystemAddress.clone().add("bootstrap-context", ctx + i);
                remove(wmAddress);
                remove(bsAddress);
            }
            reload();

        }

    }

    /**
     * Define the deployment
     *
     * @return The deployment archive
     */
    @Deployment
    public static EnterpriseArchive createDeployment() throws Exception {

        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "wm.jar");
        ja.addPackage(MultipleConnectionFactory1.class.getPackage()).addClasses(LongRunningThreadsCheckTestCase.class,
                MgmtOperationException.class, XMLElementReader.class, XMLElementWriter.class, JcaMgmtServerSetupTask.class,
                JcaMgmtBase.class);
        ja.addPackage(AbstractMgmtTestBase.class.getPackage());

        ResourceAdapterArchive ra1 = ShrinkWrap.create(ResourceAdapterArchive.class, "wm1.rar");
        ra1.addAsManifestResource(LongRunningThreadsCheckTestCase.class.getPackage(), "ra.xml", "ra.xml")
                .addAsManifestResource(LongRunningThreadsCheckTestCase.class.getPackage(), "ironjacamar1.xml",
                        "ironjacamar.xml")
                .addAsManifestResource(
                        new StringAsset(
                                "Dependencies: org.jboss.as.controller-client,org.jboss.dmr,org.jboss.as.cli,org.jboss.as.connector \n"),
                        "MANIFEST.MF");

        ResourceAdapterArchive ra2 = ShrinkWrap.create(ResourceAdapterArchive.class, "wm2.rar");
        ra2.addAsManifestResource(LongRunningThreadsCheckTestCase.class.getPackage(), "ra.xml", "ra.xml")
                .addAsManifestResource(LongRunningThreadsCheckTestCase.class.getPackage(), "ironjacamar2.xml",
                        "ironjacamar.xml")
                .addAsManifestResource(
                        new StringAsset(
                                "Dependencies: org.jboss.as.controller-client,org.jboss.dmr,org.jboss.as.cli,org.jboss.as.connector \n"),
                        "MANIFEST.MF");

        EnterpriseArchive ea = ShrinkWrap.create(EnterpriseArchive.class, "wm.ear");
        ea.addAsLibrary(ja).addAsModule(ra1).addAsModule(ra2);

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
        assertEquals(manager.getShortRunningThreadPool(), manager.getLongRunningThreadPool());
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
