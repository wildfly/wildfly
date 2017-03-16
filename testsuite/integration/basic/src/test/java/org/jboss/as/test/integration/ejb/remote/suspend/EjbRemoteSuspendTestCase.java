/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.remote.suspend;

import java.util.Hashtable;
import javax.ejb.NoSuchEJBException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that remote EJB requests are rejected when the container is suspended.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EjbRemoteSuspendTestCase {

    private static final String APP_NAME = "";
    private static final String DISTINCT_NAME = "";
    private static final String MODULE_NAME = "ejb-suspend-test-case";

    private static Context context;

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment
    public static Archive createDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        ejbJar.addPackage(EjbRemoteSuspendTestCase.class.getPackage());
        return ejbJar;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        final Hashtable props = new Hashtable();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        context = new InitialContext(props);
    }

    @Test
    @InSequence(1)
    public void testSuspendedCallRejected() throws Exception {
        final Echo localEcho = (Echo) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME + "/" + EchoBean.class.getSimpleName() + "!" + Echo.class.getName() + "?stateful");
        final String message = "Silence!";
        String echo = localEcho.echo(message);
        Assert.assertEquals(message, echo);


        ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.OP).set("suspend");
        managementClient.getControllerClient().execute(op);

        try {
            long fin = System.currentTimeMillis() + TimeoutUtil.adjust(5000);
            while (true) {
                echo = localEcho.echo(message);
                if (System.currentTimeMillis() > fin)
                    Assert.fail("call should have been rejected");
                Thread.sleep(300);
            }
        } catch (NoSuchEJBException expected) {

        } catch (Exception e) {
            Assert.fail(e.getMessage() + " thrown but NoSuchEJBException was expected");
        }
        finally {
            op = new ModelNode();
            op.get(ModelDescriptionConstants.OP).set("resume");
            managementClient.getControllerClient().execute(op);
            //we need to make sure the module availbility message has been recieved
            //(this is why we have InSequence, so avoid two sleep() calls)
            //otherwise the test might fail intermittently if the message has not been recieved when the
            //next test is started

            //this is a somewhat weird construct, basically we just wait up to 5 seconds for the connection
            //to become usable again
            long fin = System.currentTimeMillis() + 5000;
            while (true) {
                try {
                    localEcho.echo(message);
                    break;
                } catch (Exception e) {
                    if (System.currentTimeMillis() > fin) {
                        throw e;
                    }
                }
                Thread.sleep(300);
            }

        }
    }


    @Test
    @InSequence(2)
    public void testStatefulEjbCreationRejected() throws Exception {

        ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.OP).set("suspend");
        managementClient.getControllerClient().execute(op);

        try {
            long fin = System.currentTimeMillis() + TimeoutUtil.adjust(5000);
            while (true) {
                Echo localEcho = (Echo) context.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME + "/" + EchoBean.class.getSimpleName() + "!" + Echo.class.getName() + "?stateful");
                if (System.currentTimeMillis() > fin)
                    Assert.fail("call should have been rejected");
                Thread.sleep(300);
            }
        } catch (NamingException expected) {

        } finally {
            op = new ModelNode();
            op.get(ModelDescriptionConstants.OP).set("resume");
            managementClient.getControllerClient().execute(op);
        }
    }
}
