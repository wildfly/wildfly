/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.ejb3.jndi;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests that the session beans are bound to all the jndi binding names mandated by the Enterprise Beans 3.1 spec, when the Jakarta Enterprise Beans are
 * deployed within a top level .ear file
 *
 * @author Jaikiran Pai
 */
@ExtendWith(ArquillianExtension.class)
public class EarDeploymentEjbJndiBindingTestCase {

    /**
     * The app name of the deployment
     */
    private static final String APP_NAME = "ear-deployment-ejb3-binding";

    /**
     * Complete ear file name including the .ear file extension
     */
    private static final String EAR_NAME = APP_NAME + ".ear";

    /**
     * The module name of the deployment
     */
    private static final String MODULE_NAME = "ejb3-jndi-binding-test";

    /**
     * Complete jar file name including the .jar file extension
     */
    private static final String JAR_NAME = MODULE_NAME + ".jar";

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
        // create the jar containing the Jakarta Enterprise Beans
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, JAR_NAME);
        // add the entire package
        jar.addPackage(SampleSLSB.class.getPackage());

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
        String ejbName = SampleSLSB.class.getSimpleName();
        // global bindings
        // 1. local business interface
        Echo localBusinessInterface = (Echo) ctx.lookup(JAVA_GLOBAL_NAMESPACE_PREFIX + APP_NAME + "/" + MODULE_NAME + "/" + ejbName + "!" + Echo.class.getName());
        Assertions.assertNotNull(localBusinessInterface, "Null object returned for local business interface lookup in java:global namespace");
        // 2. no-interface view
        SampleSLSB noInterfaceView = (SampleSLSB) ctx.lookup(JAVA_GLOBAL_NAMESPACE_PREFIX + APP_NAME + "/" + MODULE_NAME + "/" + ejbName + "!" + SampleSLSB.class.getName());
        Assertions.assertNotNull(noInterfaceView, "Null object returned for no-interface view lookup in java:global namespace");


        // app bindings
        // 1. local business interface
        Echo localBusinessInterfaceInAppNamespace = (Echo) ctx.lookup(JAVA_APP_NAMESPACE_PREFIX + MODULE_NAME + "/" + ejbName + "!" + Echo.class.getName());
        Assertions.assertNotNull(localBusinessInterfaceInAppNamespace, "Null object returned for local business interface lookup in java:app namespace");
        // 2. no-interface view
        SampleSLSB noInterfaceViewInAppNamespace = (SampleSLSB) ctx.lookup(JAVA_APP_NAMESPACE_PREFIX + MODULE_NAME + "/" + ejbName + "!" + SampleSLSB.class.getName());
        Assertions.assertNotNull(noInterfaceViewInAppNamespace, "Null object returned for no-interface view lookup in java:app namespace");

        // module bindings
        // 1. local business interface
        Echo localBusinessInterfaceInModuleNamespace = (Echo) ctx.lookup(JAVA_MODULE_NAMESPACE_PREFIX + ejbName + "!" + Echo.class.getName());
        Assertions.assertNotNull(localBusinessInterfaceInModuleNamespace, "Null object returned for local business interface lookup in java:module namespace");
        // 2. no-interface view
        SampleSLSB noInterfaceViewInModuleNamespace = (SampleSLSB) ctx.lookup(JAVA_MODULE_NAMESPACE_PREFIX + ejbName + "!" + SampleSLSB.class.getName());
        Assertions.assertNotNull(noInterfaceViewInModuleNamespace, "Null object returned for no-interface view lookup in java:module namespace");

    }

    /**
     * Tests that all possible remote view bindings of a Stateless bean are available, when deployed through a .ear
     *
     * @throws Exception
     */
    @Test
    public void testRemoteBindingsOnSLSB() throws Exception {
        Context ctx = new InitialContext();
        String ejbName = SampleSLSB.class.getSimpleName();
        // global bindings
        // 1. remote business interface
        RemoteEcho remoteBusinessInterface = (RemoteEcho) ctx.lookup(JAVA_GLOBAL_NAMESPACE_PREFIX + APP_NAME + "/" + MODULE_NAME + "/" + ejbName + "!" + RemoteEcho.class.getName());
        Assertions.assertNotNull(remoteBusinessInterface, "Null object returned for remote business interface lookup in java:global namespace");

        // app bindings
        // 1. remote business interface
        RemoteEcho remoteBusinessInterfaceInAppNamespace = (RemoteEcho) ctx.lookup(JAVA_APP_NAMESPACE_PREFIX + MODULE_NAME + "/" + ejbName + "!" + RemoteEcho.class.getName());
        Assertions.assertNotNull(remoteBusinessInterfaceInAppNamespace, "Null object returned for remote business interface lookup in java:app namespace");

        // module bindings
        // 1. remote business interface
        RemoteEcho remoteBusinessInterfaceInModuleNamespace = (RemoteEcho) ctx.lookup(JAVA_MODULE_NAMESPACE_PREFIX + ejbName + "!" + RemoteEcho.class.getName());
        Assertions.assertNotNull(remoteBusinessInterfaceInModuleNamespace, "Null object returned for remote business interface lookup in java:module namespace");

    }

    /**
     * Tests that all possible local view bindings of a Stateful bean are available, when deployed through a .ear
     *
     * @throws Exception
     */
    @Test
    public void testLocalBindingsOnSFSB() throws Exception {
        Context ctx = new InitialContext();
        String ejbName = SampleSFSB.class.getSimpleName();
        // global bindings
        // 1. local business interface
        Echo localBusinessInterface = (Echo) ctx.lookup(JAVA_GLOBAL_NAMESPACE_PREFIX + APP_NAME + "/" + MODULE_NAME + "/" + ejbName + "!" + Echo.class.getName());
        Assertions.assertNotNull(localBusinessInterface, "Null object returned for local business interface lookup in java:global namespace");
        // 2. no-interface view
        SampleSFSB noInterfaceView = (SampleSFSB) ctx.lookup(JAVA_GLOBAL_NAMESPACE_PREFIX + APP_NAME + "/" + MODULE_NAME + "/" + ejbName + "!" + SampleSFSB.class.getName());
        Assertions.assertNotNull(noInterfaceView, "Null object returned for no-interface view lookup in java:global namespace");


        // app bindings
        // 1. local business interface
        Echo localBusinessInterfaceInAppNamespace = (Echo) ctx.lookup(JAVA_APP_NAMESPACE_PREFIX + MODULE_NAME + "/" + ejbName + "!" + Echo.class.getName());
        Assertions.assertNotNull(localBusinessInterfaceInAppNamespace, "Null object returned for local business interface lookup in java:app namespace");
        // 2. no-interface view
        SampleSFSB noInterfaceViewInAppNamespace = (SampleSFSB) ctx.lookup(JAVA_APP_NAMESPACE_PREFIX + MODULE_NAME + "/" + ejbName + "!" + SampleSFSB.class.getName());
        Assertions.assertNotNull(noInterfaceViewInAppNamespace, "Null object returned for no-interface view lookup in java:app namespace");

        // module bindings
        // 1. local business interface
        Echo localBusinessInterfaceInModuleNamespace = (Echo) ctx.lookup(JAVA_MODULE_NAMESPACE_PREFIX + ejbName + "!" + Echo.class.getName());
        Assertions.assertNotNull(localBusinessInterfaceInModuleNamespace, "Null object returned for local business interface lookup in java:module namespace");
        // 2. no-interface view
        SampleSFSB noInterfaceViewInModuleNamespace = (SampleSFSB) ctx.lookup(JAVA_MODULE_NAMESPACE_PREFIX + ejbName + "!" + SampleSFSB.class.getName());
        Assertions.assertNotNull(noInterfaceViewInModuleNamespace, "Null object returned for no-interface view lookup in java:module namespace");

    }

    /**
     * Tests that all possible remote view bindings of a Stateful bean are available, when deployed through a .ear
     *
     * @throws Exception
     */
    @Test
    public void testRemoteBindingsOnSFSB() throws Exception {
        Context ctx = new InitialContext();
        String ejbName = SampleSFSB.class.getSimpleName();
        // global bindings
        // 1. remote business interface
        RemoteEcho remoteBusinessInterface = (RemoteEcho) ctx.lookup(JAVA_GLOBAL_NAMESPACE_PREFIX + APP_NAME + "/" + MODULE_NAME + "/" + ejbName + "!" + RemoteEcho.class.getName());
        Assertions.assertNotNull(remoteBusinessInterface, "Null object returned for remote business interface lookup in java:global namespace");

        // app bindings
        // 1. remote business interface
        RemoteEcho remoteBusinessInterfaceInAppNamespace = (RemoteEcho) ctx.lookup(JAVA_APP_NAMESPACE_PREFIX + MODULE_NAME + "/" + ejbName + "!" + RemoteEcho.class.getName());
        Assertions.assertNotNull(remoteBusinessInterfaceInAppNamespace, "Null object returned for remote business interface lookup in java:app namespace");

        // module bindings
        // 1. remote business interface
        RemoteEcho remoteBusinessInterfaceInModuleNamespace = (RemoteEcho) ctx.lookup(JAVA_MODULE_NAMESPACE_PREFIX + ejbName + "!" + RemoteEcho.class.getName());
        Assertions.assertNotNull(remoteBusinessInterfaceInModuleNamespace, "Null object returned for remote business interface lookup in java:module namespace");

    }

}
