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
package org.jboss.as.test.integration.jca.basic;

import java.util.List;
import javax.annotation.Resource;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.connector.subsystems.resourceadapters.Namespace;
import org.jboss.as.connector.subsystems.resourceadapters.ResourceAdapterSubsystemParser;
import org.jboss.as.test.integration.jca.basic.BasicDeployment16TestCase.BasicDeploymentTestCaseSetup;
import org.jboss.as.test.integration.jca.rar.MultipleAdminObject1;
import org.jboss.as.test.integration.jca.rar.MultipleConnectionFactory1;
import org.jboss.as.test.integration.management.base.AbstractMgmtServerSetupTask;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.as.test.shared.FileUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Basic test to check if we can activate more than one IDed RA. (there are similar tests but with exploded module)
 *
 * @author baranowb
 */
@RunWith(Arquillian.class)
@ServerSetup(BasicDoubleDeployment16TestCase.BasicDoubleDeploymentTestCaseSetup.class)
public class BasicDoubleDeployment16TestCase extends ContainerResourceMgmtTestBase {

    // deployment archive name must match "archive" element.
    private static final String DEPLOYMENT_MODULE_NAME = "basic";
    private static final String DEPLOYMENT_NAME = DEPLOYMENT_MODULE_NAME + ".rar";
    private static final String SUB_DEPLOYMENT_MODULE_NAME = "somejar";
    private static final String SUB_DEPLOYMENT_NAME = SUB_DEPLOYMENT_MODULE_NAME + ".jar";
    private static final String RAR_1_NAME = "XXX1";
    private static final String RAR_2_NAME = "XXX2";

    static class BasicDoubleDeploymentTestCaseSetup extends AbstractMgmtServerSetupTask {

        @Override
        public void doSetup(final ManagementClient managementClient) throws Exception {
            String xml = FileUtils.readFile(BasicDeployment16TestCase.class, "basic16.1.xml");
            List<ModelNode> operations = xmlToModelOperations(xml, Namespace.RESOURCEADAPTERS_1_1.getUriString(),
                    new ResourceAdapterSubsystemParser());
            executeOperation(operationListToCompositeOperation(operations));
            String xml2 = FileUtils.readFile(BasicDeployment16TestCase.class, "basic16.1-2.xml");
            List<ModelNode> operations2 = xmlToModelOperations(xml2, Namespace.RESOURCEADAPTERS_1_1.getUriString(),
                    new ResourceAdapterSubsystemParser());
            executeOperation(operationListToCompositeOperation(operations2));

        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {

            try {
                remove(getAddress(RAR_1_NAME));
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                remove(getAddress(RAR_2_NAME));
            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        private ModelNode getAddress(final String name) {
            final ModelNode address = new ModelNode();
            address.add("subsystem", "resource-adapters");
            address.add("resource-adapter", name);
            address.protect();
            return address;
        }
    }

    @Deployment
    public static ResourceAdapterArchive createDeployment() {
        ResourceAdapterArchive raa = ShrinkWrap.create(ResourceAdapterArchive.class, DEPLOYMENT_NAME);
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, SUB_DEPLOYMENT_NAME);
        ja.addPackage(MultipleConnectionFactory1.class.getPackage()).addClasses(MgmtOperationException.class,
                XMLElementReader.class, XMLElementWriter.class, BasicDoubleDeployment16TestCase.class,
                BasicDeploymentTestCaseSetup.class);
        ja.addPackage(AbstractMgmtTestBase.class.getPackage());

        raa.addAsLibrary(ja);

        raa.addAsManifestResource(BasicDeployment16TestCase.class.getPackage(), "ra16.xml", "ra.xml")
                .addAsManifestResource(
                        new StringAsset("Dependencies: org.jboss.as.controller-client,org.jboss.dmr,org.jboss.as.cli\n"),
                        "MANIFEST.MF");
        return raa;
    }

    @Resource(mappedName = "java:jboss/name1")
    private MultipleConnectionFactory1 connectionFactory1;

    @Resource(mappedName = "java:jboss/Name3")
    private MultipleAdminObject1 adminObject1;

    @Resource(mappedName = "java:jboss/name1-2")
    private MultipleConnectionFactory1 connectionFactory2;

    @Resource(mappedName = "java:jboss/Name3-2")
    private MultipleAdminObject1 adminObject2;

    /**
     * Test configuration
     *
     * @throws Throwable Thrown if case of an error
     */
    @Test
    public void testConfiguration() throws Throwable {
        Assert.assertNotNull("CF1 not found", connectionFactory1);
        Assert.assertNotNull("AO1 not found", adminObject1);
        Assert.assertNotNull("CF2 not found", connectionFactory2);
        Assert.assertNotNull("AO2 not found", adminObject2);
    }

}
