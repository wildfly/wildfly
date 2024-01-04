/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remote.byreference;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.integration.ejb.remote.common.EJBManagementUtil;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that the EJB subsystem can be configured for pass-by-reference semantics for in-vm invocations on
 * remote interfaces of EJBs
 *
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
@ServerSetup(RemoteInvocationByReferenceTestCase.RemoteInvocationByReferenceTestCaseSetup.class)
public class RemoteInvocationByReferenceTestCase {

    private static final String ARCHIVE_NAME = "in-vm-remote-interface-pass-by-reference-test";

    static class RemoteInvocationByReferenceTestCaseSetup implements ServerSetupTask {

        @Override
        public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
            // setup pass-by-reference semantics
            // we do this here instead of a separate @BeforeClass method because of some weirdness
            // with the ordering of the @BeforeClass execution and deploying the deployment by Arquillian
            EJBManagementUtil.disablePassByValueForRemoteInterfaceInvocations(managementClient);
        }

        @Override
        public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
            // switch back to the default pass-by-value semantics
            EJBManagementUtil.undefinePassByValueForRemoteInterfaceInvocations(managementClient);
        }
    }

    @ArquillianResource
    private InitialContext iniCtx;

    @Deployment
    public static Archive<?> createDeployment() {
        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addClasses(StatelessRemoteBean.class, RemoteInterface.class, RemoteInvocationByReferenceTestCaseSetup.class, RemoteByReferenceException.class, NonSerializableObject.class,
                HelloBean.class, HelloRemote.class, TransferParameter.class, TransferReturnValue.class, SerializableObject.class);
        return jar;
    }

    protected <T> T lookup(String beanName, Class<T> interfaceType) throws NamingException {
        return interfaceType.cast(iniCtx.lookup("java:global/" + ARCHIVE_NAME + "/" + beanName + "!" + interfaceType.getName()));
    }

    /**
     * Test that invocation on a remote interface of an EJB uses pass-by-reference semantics
     *
     * @throws Exception
     */
    @Test
    public void testPassByReferenceSemanticsOnRemoteInterface() throws Exception {
        final String[] array = {"hello"};
        final RemoteInterface remote = lookup(StatelessRemoteBean.class.getSimpleName(), RemoteInterface.class);
        final String newValue = "foo";
        // invoke on the remote interface
        remote.modifyFirstElementOfArray(array, newValue);
        Assert.assertEquals("Invocation on remote interface of an EJB did *not* use pass-by-reference semantics", newValue, array[0]);
    }

    /**
     * Test that invocation on a remote interface of an EJB uses pass-by-reference semantics and the Exception also is pass-by-reference
     * @throws Exception
     */
    @Test
    public void testPassByReferenceObjectAndException() throws Exception {
        final HelloRemote remote = lookup(HelloBean.class.getSimpleName(), HelloRemote.class);
        // invoke on the remote interface
        TransferReturnValue ret = remote.hello(new TransferParameter(this.getClass().getSimpleName()));
        Assert.assertEquals("Invocation on remote interface of Hello did *not* use pass-by-reference semantics", ret.getValue(), "Hello " + this.getClass().getSimpleName());

        try {
            remote.hello(null);
        } catch(RemoteByReferenceException he) {
            Assert.assertEquals("Invocation on remote interface of an EJB did *not* use pass-by-reference in exception", he.getMessage(), "Param was null");
        }
    }

    @Test
    public void testPassByReferenceNonSerializableAndException() throws Exception {
        final HelloRemote remote = lookup(HelloBean.class.getSimpleName(), HelloRemote.class);
        // invoke on the remote interface
        NonSerializableObject ret = remote.helloNonSerializable(new NonSerializableObject("Hello"));
        Assert.assertEquals("Invocation on remote interface of Hello did *not* use pass-by-reference semantics", ret.getValue(), "Bye");

        try {
            remote.helloNonSerializable(null);
        } catch(RemoteByReferenceException he) {
            Assert.assertEquals("Invocation on remote interface of an EJB did *not* use pass-by-reference in exception", he.getMessage(), "Param was null");
        }
    }

    @Test
    public void testPassByReferenceSerializable() throws Exception {
        final HelloRemote remote = lookup(HelloBean.class.getSimpleName(), HelloRemote.class);
        // invoke on the remote interface
        SerializableObject ret = remote.helloSerializable(new SerializableObject("Hello"));
        Assert.assertEquals("Invocation on remote interface of Hello did *not* use pass-by-reference semantics", ret.getValue(), "Bye");

        try {
            remote.helloSerializable(null);
        } catch(RemoteByReferenceException he) {
            Assert.assertEquals("Invocation on remote interface of an EJB did *not* use pass-by-reference in exception", he.getMessage(), "Param was null");
        }
    }

    @Test
    public void testPassByReferenceSerializableToNonSerializable() throws Exception {
        final HelloRemote remote = lookup(HelloBean.class.getSimpleName(), HelloRemote.class);
        // invoke on the remote interface
        NonSerializableObject ret = remote.helloSerializableToNonSerializable(new SerializableObject("Hello"));
        Assert.assertEquals("Invocation on remote interface of Hello did *not* use pass-by-reference semantics", ret.getValue(), "Bye");

        try {
            remote.helloSerializableToNonSerializable(null);
        } catch(RemoteByReferenceException he) {
            Assert.assertEquals("Invocation on remote interface of an EJB did *not* use pass-by-reference in exception", he.getMessage(), "Param was null");
        }
    }

    @Test
    public void testPassByReferenceNonSerializableToSerializable() throws Exception {
        final HelloRemote remote = lookup(HelloBean.class.getSimpleName(), HelloRemote.class);
        // invoke on the remote interface
        SerializableObject ret = remote.helloNonSerializableToSerializable(new NonSerializableObject("Hello"));
        Assert.assertEquals("Invocation on remote interface of Hello did *not* use pass-by-reference semantics", ret.getValue(), "Bye");

        try {
            remote.helloNonSerializableToSerializable(null);
        } catch(RemoteByReferenceException he) {
            Assert.assertEquals("Invocation on remote interface of an EJB did *not* use pass-by-reference in exception", he.getMessage(), "Param was null");
        }
    }
}
