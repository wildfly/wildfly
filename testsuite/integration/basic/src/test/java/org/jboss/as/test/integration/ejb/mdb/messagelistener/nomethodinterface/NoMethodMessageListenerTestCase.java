/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.mdb.messagelistener.nomethodinterface;

import java.util.concurrent.TimeUnit;
import jakarta.ejb.EJB;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * EJB 3.2 5.4.3:
 * A message-driven bean is permitted to implement a listener interface with no methods. A bean that
 * implements a no-methods interface, exposes all non-static public methods of the bean class and
 * of any superclasses except java.lang.Object as message listener methods.
 * In this case, when requested by a resource adapter, the container provides a proxy which implements the
 * message listener interface and all message listener methods of the bean. A resource adapter may use the
 * Reflection API to invoke a message listener method on such a proxy.
 *
 * @author Jan Martiska
 */
@RunWith(Arquillian.class)
public class NoMethodMessageListenerTestCase {

    public static final String EAR_NAME = "no-method-message-listener-test";
    private static final String RAR_NAME = "resource-adapter";
    private static final String EJB_JAR_NAME = "message-driven-bean";
    private static final String LIB_JAR_NAME = "common";

    @EJB
    private ReceivedMessageTracker tracker;


    @Deployment
    public static Archive createDeployment() {
        final Package currentPackage = NoMethodMessageListenerTestCase.class.getPackage();

        final JavaArchive rar = ShrinkWrap.create(JavaArchive.class, RAR_NAME + ".rar")
                .addAsManifestResource(currentPackage, "ra.xml", "ra.xml");

        final JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, EJB_JAR_NAME + ".jar")
                .addClasses(SimpleMessageDrivenBean.class, NoMethodMessageListenerTestCase.class, ReceivedMessageTracker.class);

        final JavaArchive libJar = ShrinkWrap.create(JavaArchive.class, LIB_JAR_NAME + ".jar")
                .addClasses(SimpleActivationSpec.class, SimpleResourceAdapter.class)
                .addClasses(NoMethodMessageListener.class, TimeoutUtil.class);

        final EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, EAR_NAME + ".ear")
                .addAsModule(rar)
                .addAsModule(ejbJar)
                .addAsLibrary(libJar)
                .addAsManifestResource(currentPackage, "permissions.xml", "permissions.xml");

        return ear;
    }

    /**
     * The resource adapter is programmed to send a message to the MDB right after the MDB endpoint is activated.
     * Therefore, no actions except deploying the EAR are needed.
     *
     * @throws InterruptedException
     */
    @Test
    public void doTest() throws InterruptedException {
        boolean receivedSuccessfully = tracker.getReceivedLatch()
                .await(TimeoutUtil.adjust(30), TimeUnit.SECONDS);
        Assert.assertTrue("Message was not received within reasonable timeout", receivedSuccessfully);
    }


}
