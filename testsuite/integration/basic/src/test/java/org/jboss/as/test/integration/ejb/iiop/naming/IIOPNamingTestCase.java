package org.jboss.as.test.integration.ejb.iiop.naming;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.Properties;

import javax.ejb.RemoveException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ContainerResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Stuart Douglas
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class IIOPNamingTestCase {

    @ContainerResource
    private ManagementClient managementClient;

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        System.setProperty("com.sun.CORBA.ORBUseDynamicStub", "true");
        return ShrinkWrap.create(JavaArchive.class, "test.jar")
                .addPackage(IIOPNamingTestCase.class.getPackage())
                .addAsManifestResource(IIOPNamingTestCase.class.getPackage(), "jboss-ejb3.xml", "jboss-ejb3.xml");
    }

    @Deployment(name="test2", testable = false)
    public static Archive<?> descriptorOverrideDeploy() {
        return ShrinkWrap.create(JavaArchive.class, "test2.jar")
                .addClasses(IIOPNamingHome.class, IIOPRemote.class, IIOPNamingBean.class)
                .addAsManifestResource(IIOPNamingTestCase.class.getPackage(), "jboss-ejb3-naming.xml", "jboss-ejb3.xml");
    }

    private static String property(final String name) {
        return System.getProperty(name);
    }

    @Test
    public void testIIOPNamingInvocation() throws NamingException, RemoteException {
        final Properties prope = new Properties();
        prope.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.cosnaming.CNCtxFactory");
        prope.put(Context.PROVIDER_URL, "corbaloc::" +managementClient.getMgmtAddress() + ":3528/NameService");
        final InitialContext context = new InitialContext(prope);
        final Object iiopObj = context.lookup("IIOPNamingBean");
        final IIOPNamingHome object = (IIOPNamingHome) PortableRemoteObject.narrow(iiopObj, IIOPNamingHome.class);
        final IIOPRemote result = object.create();
        Assert.assertEquals("hello", result.hello());
    }

    @Test
    public void testStatefulIIOPNamingInvocation() throws NamingException, RemoteException, RemoveException {
        final Properties prope = new Properties();
        prope.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.cosnaming.CNCtxFactory");
        prope.put(Context.PROVIDER_URL, "corbaloc::" + managementClient.getMgmtAddress() +":3528/NameService");
        final InitialContext context = new InitialContext(prope);
        final Object iiopObj = context.lookup("IIOPStatefulNamingBean");
        final IIOPStatefulNamingHome object = (IIOPStatefulNamingHome) PortableRemoteObject.narrow(iiopObj, IIOPStatefulNamingHome.class);
        final IIOPStatefulRemote result = object.create(10);
        Assert.assertEquals(11, result.increment());
        Assert.assertEquals(12, result.increment());
        result.remove();
        try {
            result.increment();
            Assert.fail("Expected NoSuchObjectException");
        } catch (NoSuchObjectException expected) {

        }
    }

    @Test
    public void testIIOPNamingCorbanameInvocation() throws NamingException, RemoteException {
        final Properties prope = new Properties();
        prope.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.cosnaming.CNCtxFactory");
        prope.put(Context.PROVIDER_URL, "corbaloc::" + managementClient.getMgmtAddress() +":3528");
        final InitialContext context = new InitialContext(prope);
        final Object iiopObj = context.lookup("corbaname:iiop:" + managementClient.getMgmtAddress() +":3528#IIOPNamingBean");
        final IIOPNamingHome object = (IIOPNamingHome) PortableRemoteObject.narrow(iiopObj, IIOPNamingHome.class);
        final IIOPRemote result = object.create();
        Assert.assertEquals("hello", result.hello());
    }

    @Test
    public void testStatefulIIOPNamingCorbanameInvocation() throws NamingException, RemoteException, RemoveException {
        final Properties prope = new Properties();
        prope.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.cosnaming.CNCtxFactory");
        prope.put(Context.PROVIDER_URL, "corbaloc::"  + managementClient.getMgmtAddress() + ":3528");
        final InitialContext context = new InitialContext(prope);
        final Object iiopObj = context.lookup("IIOPStatefulNamingBean");
        final IIOPStatefulNamingHome object = (IIOPStatefulNamingHome) PortableRemoteObject.narrow(iiopObj, IIOPStatefulNamingHome.class);
        final IIOPStatefulRemote result = object.create(10);
        Assert.assertEquals(11, result.increment());
        Assert.assertEquals(12, result.increment());
        result.remove();
        try {
            result.increment();
            Assert.fail("Expected NoSuchObjectException");
        } catch (NoSuchObjectException expected) {

        }
    }

    @Test
    public void testIIOPNamingIIOPInvocation() throws NamingException, RemoteException {
        final Properties prope = new Properties();
        prope.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.cosnaming.CNCtxFactory");
        prope.put(Context.PROVIDER_URL, "iiop://" + managementClient.getMgmtAddress() +":3528");
        final InitialContext context = new InitialContext(prope);
        final Object iiopObj = context.lookup("IIOPNamingBean");
        final IIOPNamingHome object = (IIOPNamingHome) PortableRemoteObject.narrow(iiopObj, IIOPNamingHome.class);
        final IIOPRemote result = object.create();
        Assert.assertEquals("hello", result.hello());
    }

    @Test
    public void testStatefulIIOPNamingIIOPInvocation() throws NamingException, RemoteException, RemoveException {
        final Properties prope = new Properties();
        prope.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.cosnaming.CNCtxFactory");
        prope.put(Context.PROVIDER_URL, "iiop://" + managementClient.getMgmtAddress() +":3528");
        final InitialContext context = new InitialContext(prope);
        final Object iiopObj = context.lookup("IIOPStatefulNamingBean");
        final IIOPStatefulNamingHome object = (IIOPStatefulNamingHome) PortableRemoteObject.narrow(iiopObj, IIOPStatefulNamingHome.class);
        final IIOPStatefulRemote result = object.create(10);
        Assert.assertEquals(11, result.increment());
        Assert.assertEquals(12, result.increment());
        result.remove();
        try {
            result.increment();
            Assert.fail("Expected NoSuchObjectException");
        } catch (NoSuchObjectException expected) {
        }
    }

    /**
     * <p>
     * Tests the corbaloc lookup of a bean that used the jboss-ejb3.xml deployment descriptor to override the COSNaming
     * binding. So, insteand of looking for the standard test2/IIOPNamingBean context we will look for the configured
     * bean/custom/name/IIOPNamingBean context.
     * </p>
     *
     * @throws NamingException if an error occurs while looking up the bean.
     * @throws RemoteException if an error occurs while invoking the remote bean.
     */
    @Test
    public void testCorbalocInvocationWithDDOverride() throws NamingException, RemoteException {
        final Properties prope = new Properties();
        prope.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.cosnaming.CNCtxFactory");
        prope.put(Context.PROVIDER_URL, "corbaloc::" + managementClient.getMgmtAddress() +":3528/NameService");
        final InitialContext context = new InitialContext(prope);
        final Object iiopObj = context.lookup("bean/custom/name/IIOPNamingBean");
        final IIOPNamingHome object = (IIOPNamingHome) PortableRemoteObject.narrow(iiopObj, IIOPNamingHome.class);
        final IIOPRemote result = object.create();
        Assert.assertEquals("hello", result.hello());
    }

    /**
     * <p>
     * Tests the corbaname lookup of a bean that used the jboss-ejb3.xml deployment descriptor to override the COSNaming
     * binding. So, insteand of looking for the standard test2/IIOPNamingBean context we will look for the configured
     * bean/custom/name/IIOPNamingBean context.
     * </p>
     *
     * @throws NamingException if an error occurs while looking up the bean.
     * @throws RemoteException if an error occurs while invoking the remote bean.
     */
    @Test
    public void testCorbanameInvocationWithDDOverride() throws NamingException, RemoteException {
        final Properties prope = new Properties();
        prope.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.cosnaming.CNCtxFactory");
        prope.put(Context.PROVIDER_URL, "corbaloc::" + managementClient.getMgmtAddress() +":3528");
        final InitialContext context = new InitialContext(prope);
        final Object iiopObj = context.lookup("corbaname:iiop:" + managementClient.getMgmtAddress() +":3528#bean/custom/name/IIOPNamingBean");
        final IIOPNamingHome object = (IIOPNamingHome) PortableRemoteObject.narrow(iiopObj, IIOPNamingHome.class);
        final IIOPRemote result = object.create();
        Assert.assertEquals("hello", result.hello());
    }

    /**
     * <p>
     * Tests the iiop lookup of a bean that used the jboss-ejb3.xml deployment descriptor to override the COSNaming
     * binding. So, insteand of looking for the standard test2/IIOPNamingBean context we will look for the configured
     * bean/custom/name/IIOPNamingBean context.
     * </p>
     *
     * @throws NamingException if an error occurs while looking up the bean.
     * @throws RemoteException if an error occurs while invoking the remote bean.
     */
    @Test
    public void testIIOPInvocationWithDDOverride() throws NamingException, RemoteException {
        final Properties prope = new Properties();
        prope.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.cosnaming.CNCtxFactory");
        prope.put(Context.PROVIDER_URL, "iiop://" + managementClient.getMgmtAddress() +":3528");
        final InitialContext context = new InitialContext(prope);
        final Object iiopObj = context.lookup("bean/custom/name/IIOPNamingBean");
        final IIOPNamingHome object = (IIOPNamingHome) PortableRemoteObject.narrow(iiopObj, IIOPNamingHome.class);
        final IIOPRemote result = object.create();
        Assert.assertEquals("hello", result.hello());
    }
}
