/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.smoke.ejb3.jndi;

import static org.junit.jupiter.api.Assertions.assertNotNull;

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
 * Tests that the session beans are bound to all the jndi binding names mandated by the EJB3.1 spec, when the EJBs are
 * deployed in a standalone jar file
 *
 * @author Jaikiran Pai
 */
@ExtendWith(ArquillianExtension.class)
public class StandaloneModuleEjbJndiBindingTestCase {

    /**
     * The module name of the deployment
     */
    private static final String MODULE_NAME = "ejb3-jndi-binding-test";

    /**
     * Complete jar file name including the .jar file extension
     */
    private static final String ARCHIVE_NAME = MODULE_NAME + ".jar";

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
    public static JavaArchive createStandaloneJar() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME);
        // add the entire package
        jar.addPackage(SampleSLSB.class.getPackage());

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
        String ejbName = SampleSLSB.class.getSimpleName();
        // global bindings
        // 1. local business interface
        Echo localBusinessInterface = (Echo) ctx.lookup(JAVA_GLOBAL_NAMESPACE_PREFIX + MODULE_NAME + "/" + ejbName + "!" + Echo.class.getName());
        Assertions.assertNotNull(localBusinessInterface, "Null object returned for local business interface lookup in java:global namespace");
        // 2. no-interface view
        SampleSLSB noInterfaceView = (SampleSLSB) ctx.lookup(JAVA_GLOBAL_NAMESPACE_PREFIX + MODULE_NAME + "/" + ejbName + "!" + SampleSLSB.class.getName());
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

        // additional binding
        {
            final Echo bean = (Echo) ctx.lookup(JAVA_GLOBAL_NAMESPACE_PREFIX + "Additional");
            assertNotNull(bean, "Null object returned from java:global/Additional");
        }
    }

    /**
     * Tests that all possible remote view bindings of a Stateless bean are available.
     *
     * @throws Exception
     */
    @Test
    public void testRemoteBindingsOnSLSB() throws Exception {
        Context ctx = new InitialContext();
        String ejbName = SampleSLSB.class.getSimpleName();
        // global bindings
        // 1. remote business interface
        RemoteEcho remoteBusinessInterface = (RemoteEcho) ctx.lookup(JAVA_GLOBAL_NAMESPACE_PREFIX + MODULE_NAME + "/" + ejbName + "!" + RemoteEcho.class.getName());
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
     * Tests that all possible local view bindings of a Stateful bean are available.
     *
     * @throws Exception
     */
    @Test
    public void testLocalBindingsOnSFSB() throws Exception {
        Context ctx = new InitialContext();
        String ejbName = SampleSFSB.class.getSimpleName();
        // global bindings
        // 1. local business interface
        Echo localBusinessInterface = (Echo) ctx.lookup(JAVA_GLOBAL_NAMESPACE_PREFIX + MODULE_NAME + "/" + ejbName + "!" + Echo.class.getName());
        Assertions.assertNotNull(localBusinessInterface, "Null object returned for local business interface lookup in java:global namespace");
        // 2. no-interface view
        SampleSFSB noInterfaceView = (SampleSFSB) ctx.lookup(JAVA_GLOBAL_NAMESPACE_PREFIX + MODULE_NAME + "/" + ejbName + "!" + SampleSFSB.class.getName());
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
     * Tests that all possible remote view bindings of a Stateful bean are available.
     *
     * @throws Exception
     */
    @Test
    public void testRemoteBindingsOnSFSB() throws Exception {
        Context ctx = new InitialContext();
        String ejbName = SampleSFSB.class.getSimpleName();
        // global bindings
        // 1. remote business interface
        RemoteEcho remoteBusinessInterface = (RemoteEcho) ctx.lookup(JAVA_GLOBAL_NAMESPACE_PREFIX + MODULE_NAME + "/" + ejbName + "!" + RemoteEcho.class.getName());
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
