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

package org.jboss.as.test.integration.ejb.packaging.multimodule;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

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
@RunWith(Arquillian.class)
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
        Assert.assertNotNull("Null object returned for local business interface lookup in java:global namespace", localBusinessInterfaceInEarLib);

        LocalBeanInterfaceInEjbJar localBusinessInterfaceInEjbJar = (LocalBeanInterfaceInEjbJar) ctx.lookup(JAVA_GLOBAL_NAMESPACE_PREFIX + APP_NAME + "/" + MODULE_NAME + "/" + ejbName + "!" + LocalBeanInterfaceInEjbJar.class.getName());
        Assert.assertNotNull("Null object returned for local business interface lookup in java:global namespace", localBusinessInterfaceInEjbJar);

        // 2. no-interface view
        MrBean noInterfaceView = (MrBean) ctx.lookup(JAVA_GLOBAL_NAMESPACE_PREFIX + APP_NAME + "/" + MODULE_NAME + "/" + ejbName + "!" + MrBean.class.getName());
        Assert.assertNotNull("Null object returned for no-interface view lookup in java:global namespace", noInterfaceView);


        // app bindings
        // 1. local business interface
        LocalBeanInterfaceInEarLib localEarLibBusinessInterfaceInAppNamespace = (LocalBeanInterfaceInEarLib) ctx.lookup(JAVA_APP_NAMESPACE_PREFIX + MODULE_NAME + "/" + ejbName + "!" + LocalBeanInterfaceInEarLib.class.getName());
        Assert.assertNotNull("Null object returned for local business interface lookup in java:app namespace", localEarLibBusinessInterfaceInAppNamespace);

        LocalBeanInterfaceInEjbJar localEjbJarBusinessInterfaceInAppNamespace = (LocalBeanInterfaceInEjbJar) ctx.lookup(JAVA_APP_NAMESPACE_PREFIX + MODULE_NAME + "/" + ejbName + "!" + LocalBeanInterfaceInEjbJar.class.getName());
        Assert.assertNotNull("Null object returned for local business interface lookup in java:app namespace", localEjbJarBusinessInterfaceInAppNamespace);


        // 2. no-interface view
        MrBean noInterfaceViewInAppNamespace = (MrBean) ctx.lookup(JAVA_APP_NAMESPACE_PREFIX + MODULE_NAME + "/" + ejbName + "!" + MrBean.class.getName());
        Assert.assertNotNull("Null object returned for no-interface view lookup in java:app namespace", noInterfaceViewInAppNamespace);

        // module bindings
        // 1. local business interface
        LocalBeanInterfaceInEarLib localEarLibBusinessInterfaceInModuleNamespace = (LocalBeanInterfaceInEarLib) ctx.lookup(JAVA_MODULE_NAMESPACE_PREFIX + ejbName + "!" + LocalBeanInterfaceInEarLib.class.getName());
        Assert.assertNotNull("Null object returned for local business interface lookup in java:module namespace", localEarLibBusinessInterfaceInModuleNamespace);

        LocalBeanInterfaceInEjbJar localEjbJarBusinessInterfaceInModuleNamespace = (LocalBeanInterfaceInEjbJar) ctx.lookup(JAVA_MODULE_NAMESPACE_PREFIX + ejbName + "!" + LocalBeanInterfaceInEjbJar.class.getName());
        Assert.assertNotNull("Null object returned for local business interface lookup in java:module namespace", localEjbJarBusinessInterfaceInModuleNamespace);

        // 2. no-interface view
        MrBean noInterfaceViewInModuleNamespace = (MrBean) ctx.lookup(JAVA_MODULE_NAMESPACE_PREFIX + ejbName + "!" + MrBean.class.getName());
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
        String ejbName = MrBean.class.getSimpleName();
        // global bindings
        // 1. remote business interface
        RemoteBeanInterfaceInEarLib remoteBusinessInterfaceInEarLib = (RemoteBeanInterfaceInEarLib) ctx.lookup(JAVA_GLOBAL_NAMESPACE_PREFIX + APP_NAME + "/" + MODULE_NAME + "/" + ejbName + "!" + RemoteBeanInterfaceInEarLib.class.getName());
        Assert.assertNotNull("Null object returned for remote business interface lookup in java:global namespace", remoteBusinessInterfaceInEarLib);

        RemoteBeanInterfaceInEjbJar remoteBusinessInterfaceInEjbJar = (RemoteBeanInterfaceInEjbJar) ctx.lookup(JAVA_GLOBAL_NAMESPACE_PREFIX + APP_NAME + "/" + MODULE_NAME + "/" + ejbName + "!" + RemoteBeanInterfaceInEjbJar.class.getName());
        Assert.assertNotNull("Null object returned for remote business interface lookup in java:global namespace", remoteBusinessInterfaceInEjbJar);


        // app bindings
        // 1. remote business interface
        RemoteBeanInterfaceInEarLib remoteEarLibBusinessInterfaceInAppNamespace = (RemoteBeanInterfaceInEarLib) ctx.lookup(JAVA_APP_NAMESPACE_PREFIX + MODULE_NAME + "/" + ejbName + "!" + RemoteBeanInterfaceInEarLib.class.getName());
        Assert.assertNotNull("Null object returned for remote business interface lookup in java:app namespace", remoteEarLibBusinessInterfaceInAppNamespace);

        RemoteBeanInterfaceInEjbJar remoteEjbJarBusinessInterfaceInAppNamespace = (RemoteBeanInterfaceInEjbJar) ctx.lookup(JAVA_APP_NAMESPACE_PREFIX + MODULE_NAME + "/" + ejbName + "!" + RemoteBeanInterfaceInEjbJar.class.getName());
        Assert.assertNotNull("Null object returned for remote business interface lookup in java:app namespace", remoteEjbJarBusinessInterfaceInAppNamespace);


        // module bindings
        // 1. remote business interface
        RemoteBeanInterfaceInEarLib remoteEarLibBusinessInterfaceInModuleNamespace = (RemoteBeanInterfaceInEarLib) ctx.lookup(JAVA_MODULE_NAMESPACE_PREFIX + ejbName + "!" + RemoteBeanInterfaceInEarLib.class.getName());
        Assert.assertNotNull("Null object returned for remote business interface lookup in java:module namespace", remoteEarLibBusinessInterfaceInModuleNamespace);

        RemoteBeanInterfaceInEjbJar remoteEjbJarBusinessInterfaceInModuleNamespace = (RemoteBeanInterfaceInEjbJar) ctx.lookup(JAVA_MODULE_NAMESPACE_PREFIX + ejbName + "!" + RemoteBeanInterfaceInEjbJar.class.getName());
        Assert.assertNotNull("Null object returned for remote business interface lookup in java:module namespace", remoteEjbJarBusinessInterfaceInModuleNamespace);

    }
}
