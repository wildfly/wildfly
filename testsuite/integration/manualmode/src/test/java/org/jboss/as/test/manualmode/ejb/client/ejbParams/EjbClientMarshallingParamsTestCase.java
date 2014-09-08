/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.manualmode.ejb.client.ejbParams;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.InitialContext;

import java.util.Hashtable;

/**
 * Tests to verify that when an unmarshalling error occurs, regarding EJB parameters, a relevant error message is displayed at
 * the client console.
 *
 * @author Panagiotis Sotiropoulos (psotirop@redhat.com)
 * @see https://issues.jboss.org/browse/WFLY-3825 for details
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EjbClientMarshallingParamsTestCase {

    private static final Logger logger = Logger.getLogger(EjbClientMarshallingParamsTestCase.class);

    private static final String MODULE_NAME = "EjbClientMarshallingParamsTest";

    private static final String DEFAULT_JBOSSAS = "default-jbossas";

    private static final String DEFAULT_AS_DEPLOYMENT = "default-jbossas-deployment";

    private static final String ERROR_MESSAGE = "issue regarding unmarshalling of EJB parameters";

    @ArquillianResource
    private ContainerController container;

    @ArquillianResource
    private Deployer deployer;

    private Context context;

    @Before
    public void before() throws Exception {

        final Hashtable<String, Object> jndiProperties = new Hashtable<String, Object>();

        jndiProperties.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");

        jndiProperties.put(Context.PROVIDER_URL, "http-remoting://localhost:8080");

        jndiProperties.put("jboss.naming.client.ejb.context", true);

        this.context = new InitialContext(jndiProperties);
    }

    @After
    public void after() throws NamingException {

        this.context.close();
    }

    @Deployment(name = DEFAULT_AS_DEPLOYMENT, managed = false, testable = false)
    @TargetsContainer(DEFAULT_JBOSSAS)
    public static Archive<?> createContainerDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        ejbJar.addClasses(Calculator.class, CalculatorRemote.class);
        return ejbJar;
    }

    /**
     * Start a server which has a remote. Deploy (X) to server. Check that if large parameters are given to an EJB method
     * invocation (eg new byte[400000000]) a relevant exception is thrown at the client console.
     *
     * @throws Exception
     */
    @Test
    public void testEjbClientMarshallingParams() throws Exception {

        try {

            // now start the main server
            this.container.start(DEFAULT_JBOSSAS);
            // deploy to this container
            this.deployer.deploy(DEFAULT_AS_DEPLOYMENT);

            final CalculatorRemote dependentBean = (CalculatorRemote) context.lookup(MODULE_NAME + "//"
                    + Calculator.class.getSimpleName() + "!" + CalculatorRemote.class.getName());
            logger.info(dependentBean.add(5, 6));
            Assert.assertEquals(dependentBean.add(5, 6), 11);

            // Large parameter is given to an EJB method invocation
            byte[] b1 = new byte[400000000];
            logger.info(dependentBean.add(b1));

        } catch (Exception e) {
            logger.info(e.getMessage());
            String content = e.getMessage();
            // Verify client console error messages are displayed, when large
            // parameters are given to an EJB method invocation
            Assert.assertTrue(content.contains(ERROR_MESSAGE));
        } finally {

            try {
                this.deployer.undeploy(DEFAULT_AS_DEPLOYMENT);
                this.container.stop(DEFAULT_JBOSSAS);
            } catch (Exception e) {
                logger.debug("Exception during container shutdown", e);
            }

        }
    }

}

