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
package org.jboss.as.test.integration.jca.bootstrap;

import javax.annotation.Resource;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.integration.jca.JcaMgmtBase;
import org.jboss.as.test.integration.jca.JcaMgmtServerSetupTask;
import org.jboss.as.test.integration.jca.rar.*;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
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
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a> JBQA-5936 custom bootstrap context deployment
 */
@RunWith(Arquillian.class)
@ServerSetup(CustomBootstrapContextTestCase.CustomBootstrapDeploymentTestCaseSetup.class)
@Ignore("AS7-4185")
public class CustomBootstrapContextTestCase extends JcaMgmtBase {

    public static String ctx = "customContext";
    public static String wm = "customWM";

    static class CustomBootstrapDeploymentTestCaseSetup extends JcaMgmtServerSetupTask {

        ModelNode wmAddress = subsystemAddress.clone().add("workmanager", wm);
        ModelNode bsAddress = subsystemAddress.clone().add("bootstrap-context", ctx);

        @Override
        public void doSetup(final ManagementClient managementClient) throws Exception {

            ModelNode operation = new ModelNode();

            try {

                operation.get(OP).set(ADD);
                operation.get(OP_ADDR).set(wmAddress);
                operation.get(NAME).set(wm);
                executeOperation(operation);

                operation = new ModelNode();
                operation.get(OP).set(ADD);
                operation.get(OP_ADDR).set(wmAddress.clone().add("short-running-threads", wm));
                operation.get("core-threads").set("20");
                operation.get("queue-length").set("20");
                operation.get("max-threads").set("20");
                executeOperation(operation);

                operation = new ModelNode();
                operation.get(OP).set(ADD);
                operation.get(OP_ADDR).set(bsAddress);
                operation.get(NAME).set(ctx);
                operation.get("workmanager").set(wm);
                executeOperation(operation);

            } catch (Exception e) {

                throw new Exception(e.getMessage() + operation, e);
            }
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {

            remove(wmAddress);
            remove(bsAddress);
            reload();

        }

    }

    /**
     * Define the deployment
     * 
     * @return The deployment archive
     */
    @Deployment
    public static ResourceAdapterArchive createDeployment() throws Exception {
        ResourceAdapterArchive raa = ShrinkWrap.create(ResourceAdapterArchive.class, "bootstrap_archive_ij.rar");
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "multiple.jar");
        ja.addPackage(MultipleConnectionFactory1.class.getPackage()).addClasses(CustomBootstrapContextTestCase.class,
                MgmtOperationException.class, XMLElementReader.class, XMLElementWriter.class, JcaMgmtServerSetupTask.class,JcaMgmtBase.class);

        ja.addPackage(AbstractMgmtTestBase.class.getPackage());
        raa.addAsLibrary(ja);

        raa.addAsManifestResource(CustomBootstrapContextTestCase.class.getPackage(),"ra.xml", "ra.xml")
                .addAsManifestResource(CustomBootstrapContextTestCase.class.getPackage(),"ironjacamar.xml", "ironjacamar.xml")
                .addAsManifestResource(
                        new StringAsset(
                                "Dependencies: org.jboss.as.controller-client,org.jboss.dmr,org.jboss.as.cli,org.jboss.as.connector \n"),
                        "MANIFEST.MF");
        ;
        return raa;
    }

    @Resource(mappedName = "java:jboss/name1")
    private MultipleConnectionFactory1 connectionFactory1;

    @Resource(mappedName = "java:jboss/Name3")
    private MultipleAdminObject1 adminObject1;

    /**
     * Test configuration
     * 
     * @throws Throwable Thrown if case of an error
     */
    @Test
    public void testConfiguration() throws Throwable {

        assertNotNull("CF1 not found", connectionFactory1);
        assertNotNull("AO1 not found", adminObject1);

        MultipleAdminObject1Impl impl = (MultipleAdminObject1Impl) adminObject1;
        MultipleResourceAdapter2 adapter = (MultipleResourceAdapter2) impl.getResourceAdapter();
        assertNotNull(adapter);
        assertEquals(wm, adapter.getWorkManagerName());
        assertEquals(ctx, adapter.getBootstrapContextName());
    }
}
