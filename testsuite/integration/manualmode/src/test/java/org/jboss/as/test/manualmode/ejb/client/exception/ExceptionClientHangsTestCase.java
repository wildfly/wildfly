/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.test.manualmode.ejb.client.exception;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

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
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Properties;

/**
 * Test case for WFLY-12919
 *
 * When exception is returned from server, WildFlyResponseClient hangs and client never receives the response.
 * Test uses two deployments. One with the EJB client and one with the remote bean throwing exception.
 * Test calls the client deployment and the client invokes the remote bean. Test should not keep hanging.
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ExceptionClientHangsTestCase {

    private static final Logger log = Logger.getLogger(ExceptionClientHangsTestCase.class);

    /**
     * New container named "jbossas-disable-assertions" is defined in arquillian.xml and manualmode-build.xml
     * with the property enableAssertions set to false.
     * We need a separate configuration in order to reproduce this issue, because enabling assertions
     * prevents the client to hang.
     */
    private static final String CONTAINER = "jbossas-disable-assertions";

    public static final String DEPLOYMENT_NAME_EJB = "DeploymentEjb";
    public static final String DEPLOYMENT_NAME_CLIENT = "DeploymentClient";

    @ArquillianResource
    private ContainerController controller;

    @ArquillianResource
    private Deployer deployer;

    @Deployment(name = DEPLOYMENT_NAME_EJB, managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static Archive<?> deploy_ejb() {
        EnterpriseArchive ear_ejb = ShrinkWrap.create(EnterpriseArchive.class, DEPLOYMENT_NAME_EJB + ".ear");
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "ejbJar.jar");
        ejbJar.addClasses(BadException.class, SimpleRemote.class, SimpleRemoteBean.class);
        ear_ejb.addAsModule(ejbJar);
        ear_ejb.as(org.jboss.shrinkwrap.api.exporter.ZipExporter.class).exportTo(new java.io.File("/tmp/" + ear_ejb.getName()), true);
        return ear_ejb;
    }

    @Deployment(name = DEPLOYMENT_NAME_CLIENT, managed = false, testable = false)
    @TargetsContainer(CONTAINER)
    public static Archive<?> deploy_client() {
        JavaArchive clientJar = ShrinkWrap.create(JavaArchive.class, DEPLOYMENT_NAME_CLIENT + ".jar");
        clientJar.addClasses(Client.class, ClientInterface.class, SimpleRemote.class);
        clientJar.as(org.jboss.shrinkwrap.api.exporter.ZipExporter.class).exportTo(new java.io.File("/tmp/" + clientJar.getName()), true);
        return clientJar;
    }

    @Before
    public void before() throws Exception {
        controller.start(CONTAINER);
        log.trace("===appserver started===");
        deployer.deploy(DEPLOYMENT_NAME_EJB);
        deployer.deploy(DEPLOYMENT_NAME_CLIENT);
        log.trace("===deployments deployed===");
    }

    @After
    public void after() throws Exception {
        try {
            if (!controller.isStarted(CONTAINER)) {
                controller.start(CONTAINER);
            }
            deployer.undeploy(DEPLOYMENT_NAME_EJB);
            deployer.undeploy(DEPLOYMENT_NAME_CLIENT);
            log.trace("===deployments undeployed===");
        } finally {
            controller.stop(CONTAINER);
            log.trace("===appserver stopped===");
        }
    }

    protected static ClientInterface getBean() throws NamingException {
        Context iniCtx = getIntialContext();
        String lookup = "ejb:/DeploymentClient/Client!org.jboss.as.test.manualmode.ejb.client.exception.ClientInterface";
        return (ClientInterface) iniCtx.lookup(lookup);
    }

    @Test
    public void testReturnExceptionFromServer() throws Exception {
            ClientInterface client = getBean();
            client.callBean();
    }

    public static Context getIntialContext() throws NamingException {
        Properties props = new Properties();
        props.put(Context.INITIAL_CONTEXT_FACTORY, "org.wildfly.naming.client.WildFlyInitialContextFactory");
        props.put(Context.PROVIDER_URL, "http://localhost:8080/wildfly-services");
        props.put(Context.SECURITY_PRINCIPAL, System.getProperty("username", "user1"));
        props.put(Context.SECURITY_CREDENTIALS, System.getProperty("password", "password1"));
        return new InitialContext(props);
    }
}
