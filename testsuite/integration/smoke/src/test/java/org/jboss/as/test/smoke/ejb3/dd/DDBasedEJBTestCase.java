/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.ejb3.dd;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Jaikiran Pai
 */
@ExtendWith(ArquillianExtension.class)
public class DDBasedEJBTestCase {

    private static final String MODULE_NAME = "dd-based-slsb";

    private static final String JAR_NAME = MODULE_NAME + ".jar";

    @Deployment
    public static JavaArchive getDeployment() throws Exception {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, JAR_NAME);
        jar.addPackage(DDBasedEJBTestCase.class.getPackage());
        jar.addPackage(DDBasedSLSB.class.getPackage());
        jar.addAsManifestResource(DDBasedEJBTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        jar.addAsManifestResource(DDBasedEJBTestCase.class.getPackage(), "MANIFEST.MF", "MANIFEST.MF");
        return jar;
    }

    /**
     * Tests that all possible local view bindings of a Stateless bean are available.
     *
     * @throws Exception
     */
    @Test
    public void testLocalBindingsOnSLSB() throws Exception {
        Context ctx = new InitialContext();
        String ejbName = DDBasedSLSB.class.getSimpleName();
        Echo bean = (Echo) ctx.lookup("java:global/" + MODULE_NAME + "/" + ejbName + "!" + Echo.class.getName());

        String msg = "Simple echo!";
        String echo = bean.echo(msg);
        Assertions.assertEquals(msg, echo, "Unexpected return message from bean");

    }

    /**
     * Tests that the overrides in the ejb-jar.xml for a SLSB are honoured, and the bean is invokable through
     * its exposed views
     *
     * @throws Exception
     */
    @Test
    public void testDDOverrideOfSLSB() throws Exception {
        Context ctx = new InitialContext();
        String ejbName = DDOverrideSLSB.class.getSimpleName();
        String jndiName = "java:global/" + MODULE_NAME + "/" + ejbName;
        Echo bean = (Echo) ctx.lookup(jndiName);

        String msg = "Another simple echo!";
        String echo = bean.echo(msg);
        Assertions.assertEquals(msg, echo, "Unexpected return message from bean");
    }

    /**
     * Tests that the ejb-jar.xml and annotations are merged correctly for a SFSB, and the bean is invokable through
     * its exposed views
     *
     * @throws Exception
     */
    @Test
    public void testPartialDDSFSB() throws Exception {
        Context ctx = new InitialContext();
        String ejbName = PartialDDSFSB.class.getSimpleName();
        String localBusinessInterfaceViewJndiName = "java:global/" + MODULE_NAME + "/" + ejbName + "!" + Echo.class.getName();
        Echo localBusinessIntfView = (Echo) ctx.lookup(localBusinessInterfaceViewJndiName);
        String msgOne = "This is message one!";
        Assertions.assertEquals(msgOne, localBusinessIntfView.echo(msgOne), "Unexpected return message from bean");

        String noInterfaceViewJndiName = "java:global/" + MODULE_NAME + "/" + ejbName + "!" + PartialDDSFSB.class.getName();
        PartialDDSFSB noInterfaceView = (PartialDDSFSB) ctx.lookup(noInterfaceViewJndiName);
        String msgTwo = "Yet another message!";
        Assertions.assertEquals(msgTwo, noInterfaceView.echo(msgTwo), "Unexpected return message from no-interface view of bean");

    }

    @Test
    public void testInterceptorsOnSingleton() throws Exception {
        Context ctx = new InitialContext();
        String ejbName = InterceptedDDBean.class.getSimpleName();
        String jndiName = "java:global/" + MODULE_NAME + "/" + ejbName + "!" + InterceptedDDBean.class.getName();
        InterceptedDDBean interceptedBean = (InterceptedDDBean) ctx.lookup(jndiName);
        String msg = "You will be intercepted!!!";
        String returnMsg = interceptedBean.echo(msg);
        String expectedReturnMsg = SimpleInterceptor.class.getName() + "#" + DDBasedInterceptor.class.getName() + "#" + msg;
        Assertions.assertEquals(expectedReturnMsg, returnMsg, "Unexpected return message from bean");

    }
}
