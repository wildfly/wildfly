/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.packaging.multimodule;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.naming.Context;
import javax.naming.InitialContext;

/**
 * Tests that a deployment containing EJB interfaces in a separate jar, than the bean implementation, is deployed correctly
 * with the correct business interface views setup for the EJB.
 *
 * @see https://issues.jboss.org/browse/AS7-1112
 *      <p/>
 *      User: Jaikiran Pai
 */
@ExtendWith(ArquillianExtension.class)
public class ViewClassPackagingTestCase {

    private static final String APP_NAME = "ejb-packaging-test";

    private static final String MODULE_NAME = "ejb-jar";

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


    @Deployment
    public static EnterpriseArchive createDeployment() {
        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        ejbJar.addClasses(MrBean.class, LocalBeanInterfaceInEjbJar.class, RemoteBeanInterfaceInEjbJar.class, ViewClassPackagingTestCase.class);

        final JavaArchive beanInterfacesLibraryJar = ShrinkWrap.create(JavaArchive.class, "bean-interfaces-library.jar");
        beanInterfacesLibraryJar.addClasses(LocalBeanInterfaceInEarLib.class, RemoteBeanInterfaceInEarLib.class);


        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, APP_NAME + ".ear");
        ear.addAsModule(ejbJar);
        ear.addAsLibrary(beanInterfacesLibraryJar);

        return ear;
    }

    /**
     * Tests that all possible local view bindings of a Stateless bean are available, when deployed through a .ear
     *
     * @throws Exception
     */
    @Test
    public void testBeanLocalViewBindings() throws Exception {
        Context ctx = new InitialContext();
        String ejbName = MrBean.class.getSimpleName();
        // global bindings
        // 1. local business interface
        LocalBeanInterfaceInEarLib localBusinessInterfaceInEarLib = (LocalBeanInterfaceInEarLib) ctx.lookup(JAVA_GLOBAL_NAMESPACE_PREFIX + APP_NAME + "/" + MODULE_NAME + "/" + ejbName + "!" + LocalBeanInterfaceInEarLib.class.getName());
        Assertions.assertNotNull(localBusinessInterfaceInEarLib, "Null object returned for local business interface lookup in java:global namespace");

        LocalBeanInterfaceInEjbJar localBusinessInterfaceInEjbJar = (LocalBeanInterfaceInEjbJar) ctx.lookup(JAVA_GLOBAL_NAMESPACE_PREFIX + APP_NAME + "/" + MODULE_NAME + "/" + ejbName + "!" + LocalBeanInterfaceInEjbJar.class.getName());
        Assertions.assertNotNull(localBusinessInterfaceInEjbJar, "Null object returned for local business interface lookup in java:global namespace");

        // 2. no-interface view
        MrBean noInterfaceView = (MrBean) ctx.lookup(JAVA_GLOBAL_NAMESPACE_PREFIX + APP_NAME + "/" + MODULE_NAME + "/" + ejbName + "!" + MrBean.class.getName());
        Assertions.assertNotNull(noInterfaceView, "Null object returned for no-interface view lookup in java:global namespace");


        // app bindings
        // 1. local business interface
        LocalBeanInterfaceInEarLib localEarLibBusinessInterfaceInAppNamespace = (LocalBeanInterfaceInEarLib) ctx.lookup(JAVA_APP_NAMESPACE_PREFIX + MODULE_NAME + "/" + ejbName + "!" + LocalBeanInterfaceInEarLib.class.getName());
        Assertions.assertNotNull(localEarLibBusinessInterfaceInAppNamespace, "Null object returned for local business interface lookup in java:app namespace");

        LocalBeanInterfaceInEjbJar localEjbJarBusinessInterfaceInAppNamespace = (LocalBeanInterfaceInEjbJar) ctx.lookup(JAVA_APP_NAMESPACE_PREFIX + MODULE_NAME + "/" + ejbName + "!" + LocalBeanInterfaceInEjbJar.class.getName());
        Assertions.assertNotNull(localEjbJarBusinessInterfaceInAppNamespace, "Null object returned for local business interface lookup in java:app namespace");


        // 2. no-interface view
        MrBean noInterfaceViewInAppNamespace = (MrBean) ctx.lookup(JAVA_APP_NAMESPACE_PREFIX + MODULE_NAME + "/" + ejbName + "!" + MrBean.class.getName());
        Assertions.assertNotNull(noInterfaceViewInAppNamespace, "Null object returned for no-interface view lookup in java:app namespace");

        // module bindings
        // 1. local business interface
        LocalBeanInterfaceInEarLib localEarLibBusinessInterfaceInModuleNamespace = (LocalBeanInterfaceInEarLib) ctx.lookup(JAVA_MODULE_NAMESPACE_PREFIX + ejbName + "!" + LocalBeanInterfaceInEarLib.class.getName());
        Assertions.assertNotNull(localEarLibBusinessInterfaceInModuleNamespace, "Null object returned for local business interface lookup in java:module namespace");

        LocalBeanInterfaceInEjbJar localEjbJarBusinessInterfaceInModuleNamespace = (LocalBeanInterfaceInEjbJar) ctx.lookup(JAVA_MODULE_NAMESPACE_PREFIX + ejbName + "!" + LocalBeanInterfaceInEjbJar.class.getName());
        Assertions.assertNotNull(localEjbJarBusinessInterfaceInModuleNamespace, "Null object returned for local business interface lookup in java:module namespace");

        // 2. no-interface view
        MrBean noInterfaceViewInModuleNamespace = (MrBean) ctx.lookup(JAVA_MODULE_NAMESPACE_PREFIX + ejbName + "!" + MrBean.class.getName());
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
        String ejbName = MrBean.class.getSimpleName();
        // global bindings
        // 1. remote business interface
        RemoteBeanInterfaceInEarLib remoteBusinessInterfaceInEarLib = (RemoteBeanInterfaceInEarLib) ctx.lookup(JAVA_GLOBAL_NAMESPACE_PREFIX + APP_NAME + "/" + MODULE_NAME + "/" + ejbName + "!" + RemoteBeanInterfaceInEarLib.class.getName());
        Assertions.assertNotNull(remoteBusinessInterfaceInEarLib, "Null object returned for remote business interface lookup in java:global namespace");

        RemoteBeanInterfaceInEjbJar remoteBusinessInterfaceInEjbJar = (RemoteBeanInterfaceInEjbJar) ctx.lookup(JAVA_GLOBAL_NAMESPACE_PREFIX + APP_NAME + "/" + MODULE_NAME + "/" + ejbName + "!" + RemoteBeanInterfaceInEjbJar.class.getName());
        Assertions.assertNotNull(remoteBusinessInterfaceInEjbJar, "Null object returned for remote business interface lookup in java:global namespace");


        // app bindings
        // 1. remote business interface
        RemoteBeanInterfaceInEarLib remoteEarLibBusinessInterfaceInAppNamespace = (RemoteBeanInterfaceInEarLib) ctx.lookup(JAVA_APP_NAMESPACE_PREFIX + MODULE_NAME + "/" + ejbName + "!" + RemoteBeanInterfaceInEarLib.class.getName());
        Assertions.assertNotNull(remoteEarLibBusinessInterfaceInAppNamespace, "Null object returned for remote business interface lookup in java:app namespace");

        RemoteBeanInterfaceInEjbJar remoteEjbJarBusinessInterfaceInAppNamespace = (RemoteBeanInterfaceInEjbJar) ctx.lookup(JAVA_APP_NAMESPACE_PREFIX + MODULE_NAME + "/" + ejbName + "!" + RemoteBeanInterfaceInEjbJar.class.getName());
        Assertions.assertNotNull(remoteEjbJarBusinessInterfaceInAppNamespace, "Null object returned for remote business interface lookup in java:app namespace");


        // module bindings
        // 1. remote business interface
        RemoteBeanInterfaceInEarLib remoteEarLibBusinessInterfaceInModuleNamespace = (RemoteBeanInterfaceInEarLib) ctx.lookup(JAVA_MODULE_NAMESPACE_PREFIX + ejbName + "!" + RemoteBeanInterfaceInEarLib.class.getName());
        Assertions.assertNotNull(remoteEarLibBusinessInterfaceInModuleNamespace, "Null object returned for remote business interface lookup in java:module namespace");

        RemoteBeanInterfaceInEjbJar remoteEjbJarBusinessInterfaceInModuleNamespace = (RemoteBeanInterfaceInEjbJar) ctx.lookup(JAVA_MODULE_NAMESPACE_PREFIX + ejbName + "!" + RemoteBeanInterfaceInEjbJar.class.getName());
        Assertions.assertNotNull(remoteEjbJarBusinessInterfaceInModuleNamespace, "Null object returned for remote business interface lookup in java:module namespace");

    }
}
