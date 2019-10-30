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
package org.jboss.as.test.integration.jca.beanvalidation;

import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.jca.JcaMgmtBase;
import org.jboss.as.test.integration.jca.JcaMgmtServerSetupTask;
import org.jboss.as.test.integration.jca.beanvalidation.ra.ValidConnectionFactory;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.ResourceAdapterArchive;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="vrastsel@redhat.com">Vladimir Rastseluev</a>
 *         JBQA-6006 - disabled bean validation
 */
@RunWith(Arquillian.class)
@ServerSetup(DisabledValidationTestCase.DisabledValidationTestCaseSetup.class)
@RunAsClient
public class DisabledValidationTestCase extends JcaMgmtBase {

    static class DisabledValidationTestCaseSetup extends JcaMgmtServerSetupTask {
        ModelNode bvAddress = subsystemAddress.clone().add("bean-validation", "bean-validation");

        @Override
        protected void doSetup(ManagementClient managementClient) throws Exception {
            writeAttribute(bvAddress, "enabled", "false");
        }

    }


    @ArquillianResource
    Deployer deployer;

    /**
     * Define the deployment
     *
     * @return The deployment archive
     */
    public static ResourceAdapterArchive createDeployment(String ij) throws Exception {
        String deploymentName = (ij != null ? ij : "valid");

        ResourceAdapterArchive raa = ShrinkWrap.create(ResourceAdapterArchive.class, deploymentName + ".rar");
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, deploymentName + ".jar");
        ja.addPackage(ValidConnectionFactory.class.getPackage()).addClasses(DisabledValidationTestCase.class,
                MgmtOperationException.class, XMLElementReader.class, XMLElementWriter.class, JcaMgmtServerSetupTask.class,
                JcaMgmtBase.class);
        raa.addAsLibrary(ja);

        raa.addAsManifestResource(DisabledValidationTestCase.class.getPackage(), "ra.xml", "ra.xml").addAsManifestResource(
                DisabledValidationTestCase.class.getPackage(), "ironjacamar" + (ij != null ? "-" + ij : "") + ".xml",
                "ironjacamar.xml");

        return raa;
    }

    public void test(String deployment) {
        deployer.deploy(deployment);
        deployer.undeploy(deployment);
    }

    @Deployment(name = "wrong-ao", managed = false)
    public static ResourceAdapterArchive createAODeployment() throws Exception {
        return createDeployment("wrong-ao");
    }

    @Test
    public void testWrongAO() {
        test("wrong-ao");
    }

    @Deployment(name = "wrong-cf", managed = false)
    public static ResourceAdapterArchive createCfDeployment() throws Exception {
        return createDeployment("wrong-cf");
    }

    @Test
    public void testWrongCf() {
        test("wrong-cf");
    }

    @Deployment(name = "wrong-ra", managed = false)
    public static ResourceAdapterArchive createRaDeployment() throws Exception {
        return createDeployment("wrong-ra");
    }

    @Test
    public void testWrongRA() {
        test("wrong-ra");
    }

}
