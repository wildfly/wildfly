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

package org.jboss.as.test.clustering.single.ejb.remotecall;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.concurrent.Future;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.clustering.EJBClientContextSelector;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Calling clustered annotated beans on one (local) machine plus testing remote calls for that single node.
 * 
 * @author Kabir Khan, Ondrej Chaloupka
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RemoteLocalCallsTestCase {
    private static final Logger log = Logger.getLogger(RemoteLocalCallsTestCase.class);
    private static final String ARCHIVE_NAME = "localcall-test";

    private static InitialContext initalContext;
    private static InitialContext ejbInitialContext;

    @Deployment(name = "localcall")
    public static Archive<?> deployment() throws Exception {
        final JavaArchive jar = ShrinkWrap.create(JavaArchive.class, ARCHIVE_NAME + ".jar");
        jar.addPackage(RemoteLocalCallsTestCase.class.getPackage());
        jar.addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller-client, org.jboss.dmr \n"),
                "MANIFEST.MF");
        log.info(jar.toString(true));
        return jar;
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        final Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        ejbInitialContext = new InitialContext(props);

    }
    
    @AfterClass
    public static void tearDown() throws Exception {
        if(initalContext != null) { 
            initalContext.close();
        }
        if(ejbInitialContext != null) { 
            ejbInitialContext.close();
        }
    }

    private InitialContext getInitialContext(String remoteProviderUrl) throws Exception {
        if(initalContext == null) {
            final Properties env = new Properties();
            env.put(Context.INITIAL_CONTEXT_FACTORY, org.jboss.naming.remote.client.InitialContextFactory.class.getName());
            env.put(Context.PROVIDER_URL, remoteProviderUrl);
            env.put("jboss.naming.client.ejb.context", true);
            env.put("jboss.naming.client.connect.options.org.xnio.Options.SASL_POLICY_NOPLAINTEXT", "false");
            env.put("jboss.naming.client.security.callback.handler.class",  org.jboss.as.test.shared.integration.ejb.security.CallbackHandler.class.getName());
            initalContext = new InitialContext(env);
        } 
        return initalContext;
    }
    
    private ContextSelector<EJBClientContext> setupEJBClientContextSelector() throws IOException {
        return EJBClientContextSelector.setup("cluster/ejb3/stateful/failover/sfsb-failover-jboss-ejb-client.properties");

    }

    @Test
    @OperateOnDeployment("localcall")
    public void testRemoteIfLocalCallsEJBctx() throws Exception {
        final ContextSelector<EJBClientContext> previousSelector = this.setupEJBClientContextSelector();
        InitialContext ctx = ejbInitialContext;
        
        try {
            String lookupStr = "ejb:/" + ARCHIVE_NAME + "//" + StatefulBean.class.getSimpleName() + "!"
                    + RemoteInterface.class.getName() + "?stateful";
            RemoteInterface stateful1 = (RemoteInterface) ctx.lookup(lookupStr);
            RemoteInterface stateful2 = (RemoteInterface) ctx.lookup(lookupStr);
    
            lookupStr = "ejb:/" +  ARCHIVE_NAME + "//" + StatefulClusteredBean.class.getSimpleName() + "!"
                    + RemoteInterface.class.getName() + "?stateful";
            RemoteInterface statefulClustered1 = (RemoteInterface) ctx.lookup(lookupStr);
            RemoteInterface statefulClustered2 = (RemoteInterface) ctx.lookup(lookupStr);
    
            lookupStr = "ejb:/" +  ARCHIVE_NAME + "//" + StatelessBean.class.getSimpleName() + "!"
                    + RemoteInterface.class.getName();
            RemoteInterface stateless1 = (RemoteInterface) ctx.lookup(lookupStr);
            RemoteInterface stateless2 = (RemoteInterface) ctx.lookup(lookupStr);
    
            lookupStr = "ejb:/" +  ARCHIVE_NAME + "//" + StatelessClusteredBean.class.getSimpleName() + "!"
                    + RemoteInterface.class.getName();
            RemoteInterface statelessClustered1 = (RemoteInterface) ctx.lookup(lookupStr);
            RemoteInterface statelessClustered2 = (RemoteInterface) ctx.lookup(lookupStr);
    
            lookupStr = "ejb:/" +  ARCHIVE_NAME + "//" + ServiceBean.class.getSimpleName() + "!"
                    + ServiceRemoteInterface.class.getName();
            ServiceRemoteInterface service1 = (ServiceRemoteInterface) ctx.lookup(lookupStr);
            ServiceRemoteInterface service2 = (ServiceRemoteInterface) ctx.lookup(lookupStr);
    
            stateful1.test();
            stateful2.test();
            statefulClustered1.test();
            statefulClustered2.test();
            stateless1.test();
            stateless2.test();
            statelessClustered1.test();
            statelessClustered2.test();
            service1.test();
            service2.test();
    
            Assert.assertFalse(stateful1.hashCode() == stateful2.hashCode());
            Assert.assertFalse(statefulClustered1.hashCode() == statefulClustered2.hashCode());
            Assert.assertTrue(stateless1.hashCode() == stateless2.hashCode());
            Assert.assertTrue(statelessClustered1.hashCode() == statelessClustered2.hashCode());
            Assert.assertTrue(service1.hashCode() == service2.hashCode());
    
            Assert.assertFalse(stateful1.equals(stateful2));
            Assert.assertFalse(statefulClustered1.equals(statefulClustered2));
            Assert.assertTrue(stateless1.equals(stateless2));
            Assert.assertTrue(statelessClustered1.equals(statelessClustered2));
            Assert.assertTrue(service1.equals(service2));
        } finally {
            // reset the selector -  @see RemoteEJBClientDDBasedSFSBFailoverTestCase
            if (previousSelector != null) {
                EJBClientContext.setSelector(previousSelector);
            }
        }
    }
    
    @Test
    @OperateOnDeployment("localcall")
    public void testRemoteIfLocalCalls(@ArquillianResource ManagementClient mngmtClient) throws Exception {
        InitialContext ctx = getInitialContext(mngmtClient.getRemoteEjbURL().toString());
        String lookupStr = ARCHIVE_NAME + "/" + StatefulBean.class.getSimpleName() + "!"
                + RemoteInterface.class.getName();
        RemoteInterface stateful1 = (RemoteInterface) ctx.lookup(lookupStr);
        RemoteInterface stateful2 = (RemoteInterface) ctx.lookup(lookupStr);

        lookupStr = ARCHIVE_NAME + "/" + StatefulClusteredBean.class.getSimpleName() + "!"
                + RemoteInterface.class.getName();
        RemoteInterface statefulClustered1 = (RemoteInterface) ctx.lookup(lookupStr);
        RemoteInterface statefulClustered2 = (RemoteInterface) ctx.lookup(lookupStr);

        lookupStr = ARCHIVE_NAME + "/" + StatelessBean.class.getSimpleName() + "!"
                + RemoteInterface.class.getName();
        RemoteInterface stateless1 = (RemoteInterface) ctx.lookup(lookupStr);
        RemoteInterface stateless2 = (RemoteInterface) ctx.lookup(lookupStr);

        lookupStr = ARCHIVE_NAME + "/" + StatelessClusteredBean.class.getSimpleName() + "!"
                + RemoteInterface.class.getName();
        RemoteInterface statelessClustered1 = (RemoteInterface) ctx.lookup(lookupStr);
        RemoteInterface statelessClustered2 = (RemoteInterface) ctx.lookup(lookupStr);

        lookupStr = ARCHIVE_NAME + "/" + ServiceBean.class.getSimpleName() + "!"
                + ServiceRemoteInterface.class.getName();
        ServiceRemoteInterface service1 = (ServiceRemoteInterface) ctx.lookup(lookupStr);
        ServiceRemoteInterface service2 = (ServiceRemoteInterface) ctx.lookup(lookupStr);

        stateful1.test();
        stateful2.test();
        statefulClustered1.test();
        statefulClustered2.test();
        stateless1.test();
        stateless2.test();
        statelessClustered1.test();
        statelessClustered2.test();
        service1.test();
        service2.test();

        Assert.assertFalse(stateful1.hashCode() == stateful2.hashCode());
        Assert.assertFalse(statefulClustered1.hashCode() == statefulClustered2.hashCode());
        Assert.assertTrue(stateless1.hashCode() == stateless2.hashCode());
        Assert.assertTrue(statelessClustered1.hashCode() == statelessClustered2.hashCode());
        Assert.assertTrue(service1.hashCode() == service2.hashCode());

        Assert.assertFalse(stateful1.equals(stateful2));
        Assert.assertFalse(statefulClustered1.equals(statefulClustered2));
        Assert.assertTrue(stateless1.equals(stateless2));
        Assert.assertTrue(statelessClustered1.equals(statelessClustered2));
        Assert.assertTrue(service1.equals(service2));

    }

    @Test
    @OperateOnDeployment("localcall")
    public void testLocalIfLocalCalls(@ArquillianResource ManagementClient mngmtClient) throws Exception {
        InitialContext ctx = getInitialContext(mngmtClient.getRemoteEjbURL().toString());
        String lookupStr = ARCHIVE_NAME + "/" + ServiceBean.class.getSimpleName() + "!"
                + ServiceRemoteInterface.class.getName();
        ServiceRemoteInterface service = (ServiceRemoteInterface) ctx.lookup(lookupStr);
        service.testLocal();
    }

    @Test
    @OperateOnDeployment("localcall")
    public void testAsynchronousCalls(@ArquillianResource ManagementClient mngmtClient) throws Exception {
        String remoteStringUrl = mngmtClient.getRemoteEjbURL().toString();
        InitialContext ctx = getInitialContext(remoteStringUrl);
        String lookupStr = ARCHIVE_NAME + "/" + StatelessAsyncClusteredBean.class.getSimpleName() + "!"
                + RemoteAsyncInterface.class.getName();
        RemoteAsyncInterface statelessClustered = (RemoteAsyncInterface) ctx.lookup(lookupStr);
        asynchronousBeanCall(statelessClustered, remoteStringUrl);

        lookupStr = ARCHIVE_NAME + "/" + StatefulAsyncClusteredBean.class.getSimpleName() + "!"
                + RemoteAsyncInterface.class.getName();
        RemoteAsyncInterface statefulClustered = (RemoteAsyncInterface) ctx.lookup(lookupStr);
        asynchronousBeanCall(statefulClustered, remoteStringUrl);
    }

    private void asynchronousBeanCall(RemoteAsyncInterface asyncClusteredBean, String remoteStringUrl) throws Exception {
        InitialContext ctx = getInitialContext(remoteStringUrl);
        SynchronizationSingletonInterface synchro = (SynchronizationSingletonInterface) ctx.lookup(
                ARCHIVE_NAME + "/" + SynchronizationSingleton.class.getSimpleName() + "!"
                + SynchronizationSingletonInterface.class.getName());

        synchro.resetLatches();
        asyncClusteredBean.resetMethodCalled();
        asyncClusteredBean.voidTest();
        synchro.countDownLatchNumber1();
        synchro.waitForLatchNumber2();
        Assert.assertTrue(asyncClusteredBean.getMethodCalled());

        Integer supposedNumber = 42;
        synchro.resetLatches();
        asyncClusteredBean.resetMethodCalled();
        Future<Integer> future = asyncClusteredBean.futureGetTest(supposedNumber);
        synchro.countDownLatchNumber1();
        Assert.assertEquals(new Integer(supposedNumber), future.get());
        Assert.assertTrue(asyncClusteredBean.getMethodCalled());
    }
}
