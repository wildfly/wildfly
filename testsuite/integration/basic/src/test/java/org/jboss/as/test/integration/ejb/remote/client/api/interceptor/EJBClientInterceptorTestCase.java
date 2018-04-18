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

package org.jboss.as.test.integration.ejb.remote.client.api.interceptor;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.common.context.ContextPermission;

/**
 * Tests that JBoss EJB API specific client side EJB interceptors work as expected
 *
 * @author Jaikiran Pai
 */
@RunWith(Arquillian.class)
public class EJBClientInterceptorTestCase {

    private static final String APP_NAME = "";
    private static final String DISTINCT_NAME = "";
    private static final String MODULE_NAME = "ejb-client-interceptor-test-module";

    @Deployment(testable = false)
    public static Archive createDeployment() {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, MODULE_NAME + ".jar");
        jar.addPackage(RemoteSFSB.class.getPackage());
        jar.addAsManifestResource(
                createPermissionsXmlAsset(new ContextPermission("org.wildfly.transaction.client.context.remote", "get")),
                "permissions.xml");

        return jar;
    }

    /**
     * @throws Exception
     */
    @Test
    @RunAsClient // run as a truly remote client
    public void testEJBClientInterceptionFromRemoteClient() throws Exception {
        // create some data that the client side interceptor will pass along during the EJB invocation
        final Map<String, Object> interceptorData = new HashMap<String, Object>();
        final String keyOne = "foo";
        final Object valueOne = "bar";
        final String keyTwo = "blah";
        final Object valueTwo = new Integer("12");

        interceptorData.put(keyOne, valueOne);
        interceptorData.put(keyTwo, valueTwo);

        final SimpleEJBClientInterceptor clientInterceptor = new SimpleEJBClientInterceptor(interceptorData);
        // get hold of the EJBClientContext and register the client side interceptor
        final EJBClientContext ejbClientContext = EJBClientContext.getCurrent().withAddedInterceptors(clientInterceptor);

        final Hashtable props = new Hashtable();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        final Context jndiContext = new InitialContext(props);
        ejbClientContext.runCallable(() -> {
            final RemoteSFSB remoteSFSB = (RemoteSFSB) jndiContext.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/" + DISTINCT_NAME
                    + "/" + SimpleSFSB.class.getSimpleName() + "!" + RemoteSFSB.class.getName() + "?stateful");

            // invoke the bean and ask it for the invocation data that it saw on the server side
            final Map<String, Object> valuesSeenOnServerSide = remoteSFSB.getInvocationData(keyOne, keyTwo);
            // make sure the server side bean was able to get the data which was passed on by the client side
            // interceptor
            Assert.assertNotNull("Server side context data was expected to be non-null", valuesSeenOnServerSide);
            Assert.assertFalse("Server side context data was expected to be non-empty", valuesSeenOnServerSide.isEmpty());
            for (final Map.Entry<String, Object> clientInterceptorDataEntry : interceptorData.entrySet()) {
                final String key = clientInterceptorDataEntry.getKey();
                final Object expectedValue = clientInterceptorDataEntry.getValue();
                Assert.assertEquals("Unexpected value in bean, on server side, via InvocationContext.getContextData() for key " + key, expectedValue, valuesSeenOnServerSide.get(key));
            }
            return null;
        });

    }

    /**
     * Tests that when the client to a remote view of a bean resides on the same server as the EJBs and invokes on it, then the
     * context data populated by the client interceptors gets passed on to the server side interceptors and the bean.
     *
     * @throws Exception
     * @see https://issues.jboss.org/browse/AS7-6356
     */
    @Test
    public void testEJBClientInterceptionFromInVMClient() throws Exception {
        final Hashtable props = new Hashtable();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        final Context jndiContext = new InitialContext(props);
        final RemoteViewInvoker remoteViewInvokingBean = (RemoteViewInvoker) jndiContext.lookup("ejb:" + APP_NAME + "/" + MODULE_NAME + "/"
                + DISTINCT_NAME + "/" + RemoteViewInvokingBean.class.getSimpleName() + "!" + RemoteViewInvoker.class.getName() + "?stateful");
        // get the data that the local bean is going to populate before invoking the remote bean
        final Map<String, Object> interceptorData = remoteViewInvokingBean.getDataSetupForInvocationContext();
        // invoke the bean and ask it for the invocation data that it saw on the server side
        final Map<String, Object> valuesSeenOnServerSide = remoteViewInvokingBean.invokeRemoteViewAndGetInvocationData(interceptorData.keySet().toArray(new String[interceptorData.size()]));
        // make sure the server side bean was able to get the data which was passed on by the client side
        // interceptor
        Assert.assertNotNull("Server side context data was expected to be non-null", valuesSeenOnServerSide);
        Assert.assertFalse("Server side context data was expected to be non-empty", valuesSeenOnServerSide.isEmpty());
        for (final Map.Entry<String, Object> clientInterceptorDataEntry : interceptorData.entrySet()) {
            final String key = clientInterceptorDataEntry.getKey();
            final Object expectedValue = clientInterceptorDataEntry.getValue();
            Assert.assertEquals("Unexpected value in bean, on server side, via InvocationContext.getContextData() for key " + key, expectedValue, valuesSeenOnServerSide.get(key));
        }
    }
}
