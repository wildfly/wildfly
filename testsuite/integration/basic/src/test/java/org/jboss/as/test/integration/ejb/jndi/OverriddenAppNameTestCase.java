/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.jndi;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test that the deployment(s) with an overridden application-name and module-name have the correct EJB
 * jndi bindings
 * <p/>
 * User: Jaikiran Pai
 */
@RunWith(Arquillian.class)
public class OverriddenAppNameTestCase {


    /**
     * The app name of the deployment
     */
    private static final String APP_NAME = "application-name-in-application.xml";

    /**
     * Complete ear file name including the .ear file extension
     */
    private static final String EAR_NAME = "overridden-application-name.ear";

    /**
     * The module name of the deployment
     */
    private static final String MODULE_NAME = "module-name-in-ejb-jar.xml";

    /**
     * Complete jar file name including the .jar file extension
     */
    private static final String JAR_NAME = "ejb.jar";

    /**
     * java:global/ namespace
     */
    private static final String JAVA_GLOBAL_NAMESPACE_PREFIX = "java:global/";

    /**
     * java:app/ namespace
     */
    private static final String JAVA_APP_NAMESPACE_PREFIX = "java:app/";

    /**
     * java:module/ namespace
     */
    private static final String JAVA_MODULE_NAMESPACE_PREFIX = "java:module/";

    /**
     * Create the deployment
     *
     * @return
     */
    @Deployment
    public static EnterpriseArchive createEar() {
        // create the top level ear
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_NAME);
        // add application.xml
        ear.addAsManifestResource(OverriddenAppNameTestCase.class.getPackage(), "application.xml", "application.xml");

        // create the jar containing the EJBs
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, JAR_NAME);
        // add ejb-jar.xml
        jar.addAsManifestResource(OverriddenAppNameTestCase.class.getPackage(), "ejb-jar.xml", "ejb-jar.xml");
        // add the entire package
        jar.addPackage(EchoBean.class.getPackage());

        // add the jar to the .ear
        ear.add(jar, "/", ZipExporter.class);

        // return the .ear
        return ear;
    }

    /**
     * Tests that all possible local view bindings of a Stateless bean are available, when deployed through a .ear
     *
     * @throws Exception
     */
    @Test
    public void testLocalBindingsOnSLSB() throws Exception {
        Context ctx = new InitialContext();
        String ejbName = EchoBean.class.getSimpleName();
        // global bindings
        // 1. local business interface
        Echo localBusinessInterface = (Echo) ctx.lookup(JAVA_GLOBAL_NAMESPACE_PREFIX + APP_NAME + "/" + MODULE_NAME + "/" + ejbName + "!" + Echo.class.getName());
        Assert.assertNotNull("Null object returned for local business interface lookup in java:global namespace", localBusinessInterface);
        // 2. no-interface view
        EchoBean noInterfaceView = (EchoBean) ctx.lookup(JAVA_GLOBAL_NAMESPACE_PREFIX + APP_NAME + "/" + MODULE_NAME + "/" + ejbName + "!" + EchoBean.class.getName());
        Assert.assertNotNull("Null object returned for no-interface view lookup in java:global namespace", noInterfaceView);


        // app bindings
        // 1. local business interface
        Echo localBusinessInterfaceInAppNamespace = (Echo) ctx.lookup(JAVA_APP_NAMESPACE_PREFIX + MODULE_NAME + "/" + ejbName + "!" + Echo.class.getName());
        Assert.assertNotNull("Null object returned for local business interface lookup in java:app namespace", localBusinessInterfaceInAppNamespace);
        // 2. no-interface view
        EchoBean noInterfaceViewInAppNamespace = (EchoBean) ctx.lookup(JAVA_APP_NAMESPACE_PREFIX + MODULE_NAME + "/" + ejbName + "!" + EchoBean.class.getName());
        Assert.assertNotNull("Null object returned for no-interface view lookup in java:app namespace", noInterfaceViewInAppNamespace);

        // module bindings
        // 1. local business interface
        Echo localBusinessInterfaceInModuleNamespace = (Echo) ctx.lookup(JAVA_MODULE_NAMESPACE_PREFIX + ejbName + "!" + Echo.class.getName());
        Assert.assertNotNull("Null object returned for local business interface lookup in java:module namespace", localBusinessInterfaceInModuleNamespace);
        // 2. no-interface view
        EchoBean noInterfaceViewInModuleNamespace = (EchoBean) ctx.lookup(JAVA_MODULE_NAMESPACE_PREFIX + ejbName + "!" + EchoBean.class.getName());
        Assert.assertNotNull("Null object returned for no-interface view lookup in java:module namespace", noInterfaceViewInModuleNamespace);

    }

    /**
     * Tests that all possible remote view bindings of a Stateless bean are available, when deployed through a .ear
     *
     * @throws Exception
     */
    @Test
    public void testRemoteBindingsOnSLSB() throws Exception {
        Context ctx = new InitialContext();
        String ejbName = EchoBean.class.getSimpleName();
        // global bindings
        // 1. remote business interface
        RemoteEcho remoteBusinessInterface = (RemoteEcho) ctx.lookup(JAVA_GLOBAL_NAMESPACE_PREFIX + APP_NAME + "/" + MODULE_NAME + "/" + ejbName + "!" + RemoteEcho.class.getName());
        Assert.assertNotNull("Null object returned for remote business interface lookup in java:global namespace", remoteBusinessInterface);

        // app bindings
        // 1. remote business interface
        RemoteEcho remoteBusinessInterfaceInAppNamespace = (RemoteEcho) ctx.lookup(JAVA_APP_NAMESPACE_PREFIX + MODULE_NAME + "/" + ejbName + "!" + RemoteEcho.class.getName());
        Assert.assertNotNull("Null object returned for remote business interface lookup in java:app namespace", remoteBusinessInterfaceInAppNamespace);

        // module bindings
        // 1. remote business interface
        RemoteEcho remoteBusinessInterfaceInModuleNamespace = (RemoteEcho) ctx.lookup(JAVA_MODULE_NAMESPACE_PREFIX + ejbName + "!" + RemoteEcho.class.getName());
        Assert.assertNotNull("Null object returned for remote business interface lookup in java:module namespace", remoteBusinessInterfaceInModuleNamespace);

    }

}
