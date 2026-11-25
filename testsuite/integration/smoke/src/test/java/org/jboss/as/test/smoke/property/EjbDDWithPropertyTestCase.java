/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.property;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author John Bailey
 */
@ExtendWith(ArquillianExtension.class)
@ServerSetup(EjbDDWithPropertyTestCase.EjbDDWithPropertyTestCaseSeverSetup.class)
public class EjbDDWithPropertyTestCase {
    private static final String MODULE_NAME = "dd-based";

    private static final String JAR_NAME = MODULE_NAME + ".jar";

    public static class EjbDDWithPropertyTestCaseSeverSetup implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            final ModelNode op = new ModelNode();
            op.get(OP_ADDR).set(SUBSYSTEM, "ee");
            op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            op.get(NAME).set("spec-descriptor-property-replacement");
            op.get(VALUE).set(true);
            managementClient.getControllerClient().execute(op);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            final ModelNode op = new ModelNode();
            op.get(OP_ADDR).set(SUBSYSTEM, "ee");
            op.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            op.get(NAME).set("spec-descriptor-property-replacement");
            op.get(VALUE).set(false);
            managementClient.getControllerClient().execute(op);
        }
    }


    @Deployment
    public static Archive getDeployment() throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, JAR_NAME);
        jar.addPackage(TestSessionBean.class.getPackage());
        jar.addAsManifestResource(TestSessionBean.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "test.ear");
        ear.addAsModule(jar);
        ear.addAsManifestResource(EjbDDWithPropertyTestCase.class.getPackage(), "application.xml", "application.xml");
        ear.addAsManifestResource(TestSessionBean.class.getPackage(), "jboss.properties", "jboss.properties");
        return ear;
    }

    @Test
    public void testPropertyBasedEnvEntry() throws Exception {
        Context ctx = new InitialContext();
        String ejbName = TestSessionBean.class.getSimpleName();
        TestBean bean = (TestBean) ctx.lookup("java:global/test/" + MODULE_NAME + "/" + ejbName + "!" + TestBean.class.getName());
        assertEquals("foo" + System.getProperty("file.separator") + "bar", bean.getValue());
    }

    @Test
    public void testPropertyBasedEnvEntryWithOverride() throws Exception {
        Context ctx = new InitialContext();
        String ejbName = TestSessionBean.class.getSimpleName();
        TestBean bean = (TestBean) ctx.lookup("java:global/test/" + MODULE_NAME + "/" + ejbName + "!" + TestBean.class.getName());
        assertEquals("foo-|-bar", bean.getValueOverride());
    }


    @Test
    public void testApplicationXmlEnvEntry() throws Exception {
        Context ctx = new InitialContext();
        String value = (String) ctx.lookup("java:app/value");
        assertEquals("foo" + System.getProperty("file.separator") + "bar", value);
    }
}
