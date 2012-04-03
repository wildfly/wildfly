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
package org.jboss.as.test.classloader.leak;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;
import javax.naming.InitialContext;
import junit.framework.Assert;
import org.jboss.as.test.classloader.leak.ejb.*;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;


/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
public class AbstractClassloaderLeakTestBase {

    private static final Logger log = Logger.getLogger(AbstractClassloaderLeakTestBase.class);
     
    public static final String DEPLOYMENT_DRIVER_ID = "deployment_drv";
    public static final String DEPLOYMENT_DRIVER_NAME = "ClassloaderLeakTestCase.war";
    public static final String DEPLOYMENT_APP_ID = "deployment_app";
    protected static final String NL = System.getProperty("line.separator");
    protected static final String webURI = "http://" + TestSuiteEnvironment.getServerAddress() + ":8080";
    private static final String PERSISTENCE_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?> "
            + "<persistence xmlns=\"http://java.sun.com/xml/ns/persistence\" version=\"1.0\">"
            + "  <persistence-unit name=\"test-unit\">"
            + "    <description>Persistence Unit."
            + "    </description>"
            + "  <jta-data-source>java:jboss/datasources/ExampleDS</jta-data-source>"
            + "<properties> <property name=\"hibernate.hbm2ddl.auto\" value=\"create-drop\"/>"
            + "<property name=\"jboss.entity.manager.factory.jndi.name\" value=\"testEMF\" />"
            + "</properties>"
            + "  </persistence-unit>"
            + "</persistence>";

    protected void assertClassloaderNotRegistered() {
        Assert.assertNull(ClassLoaderRef.getClassLoaderRef());
        Assert.assertNull(ClassLoaderRef.getModuleRef());
    }

    protected void assertClassloaderRegistered() {
        Assert.assertNotNull(ClassLoaderRef.getClassLoaderRef().get());
        Assert.assertNotNull(ClassLoaderRef.getModuleRef().get());
    }

    protected void assertClassloaderReleased() {

        // force garbage collector and let the VM release tested module and its classloader
        System.gc();

        if (ClassLoaderRef.getClassLoaderRef().get() != null) {
            fillMemory(ClassLoaderRef.getClassLoaderRef());
        }
        if (ClassLoaderRef.getClassLoaderRef().get() != null) {
            fillMemory(ClassLoaderRef.getClassLoaderRef());
        }

        Assert.assertNull(ClassLoaderRef.getClassLoaderRef().get());
        Assert.assertNull(ClassLoaderRef.getModuleRef().get());
    }
    
    protected void clearClassloaderRefs() {
        ClassLoaderRef.setClassLoaderRef(null);
        ClassLoaderRef.setModuleRef(null);
    }

    protected static WebArchive prepareDriverWar() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, DEPLOYMENT_DRIVER_NAME);
        war.addClass(AbstractClassloaderLeakTestBase.class);
        war.addClass(ClassLoaderRef.class);
        war.addClass(HttpRequest.class);
        war.addClass(TestSuiteEnvironment.class);
        return war;
    }

    protected static WebArchive prepareTestAppWar(String deploymentName) {
        WebArchive war = ShrinkWrap.create(WebArchive.class, deploymentName + ".war");
        war.addClass(ClassLoaderServlet.class);
        war.addAsWebResource(new StringAsset("Hello world!"), "/index.jsp");
        return war;
    }

    protected static EnterpriseArchive prepareTestAppEar(String deploymentName, WebArchive war) {
        return prepareTestAppEar(deploymentName, war, false);
    }    
    protected static EnterpriseArchive prepareTestAppEar(String deploymentName, WebArchive war, boolean clustered) {
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, deploymentName + ".ear");
        ear.addAsModule(war);

        JavaArchive jar = ShrinkWrap.create(JavaArchive.class, deploymentName + "-ejb.jar");
        jar.addClass(StatelessRemote.class);
        jar.addClass(clustered ? ClusteredStatelessBean.class : StatelessBean.class);
        jar.addClass(StatefulRemote.class);
        jar.addClass(clustered ? ClusteredStatefulBean.class : StatefulBean.class);
        jar.addClass(EntityBean.class);
        jar.addAsResource(new StringAsset(PERSISTENCE_XML), "META-INF/persistence.xml");

        ear.addAsModule(jar);

        return ear;
    }

    protected void testWar(String webURI) throws Exception {

        // access the servlet
        String response = HttpRequest.get(webURI + "/ClassloaderServlet", 10, TimeUnit.SECONDS);
        Assert.assertTrue(response.contains("ClassloaderServlet"));

        // access the JSP
        response = HttpRequest.get(webURI + "/index.jsp", 10, TimeUnit.SECONDS);
        Assert.assertTrue(response.contains("Hello world!"));

    }

    protected void testEjb(String deploymentName) throws Exception {
        testEjb(deploymentName, false);
    }
    
    protected void testEjb(String deploymentName, boolean clustered) throws Exception {
        InitialContext ctx = new InitialContext();
        StatelessRemote stateless = (StatelessRemote) ctx.lookup(
                "java:global/" + deploymentName + "/" + deploymentName + 
                    (clustered ? "-ejb/ClusteredStatelessBean" : "-ejb/StatelessBean"));

        Long id = stateless.createEntity("Hello world!");
        String response = stateless.getEntity(id);
        Assert.assertTrue(response.contains("Hello world!"));

        StatefulRemote stateful = (StatefulRemote) ctx.lookup(
                "java:global/" + deploymentName + "/" + deploymentName +
                    (clustered ? "-ejb/ClusteredStatefulBean" : "-ejb/StatefulBean"));
        stateful.setState("Hello world!");
        response = (String) stateful.getState();
        Assert.assertTrue(response.contains("Hello world!"));

    }

    private void fillMemory(WeakReference<ClassLoader> ref) {
        Runtime rt = Runtime.getRuntime();
        int[] adds = {0, 10, 20, 30, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49};
        for (int i = 0; i < adds.length; i++) {
            int toAdd = adds[i];
            System.gc();
            System.runFinalization();

            if (ref.get() == null) {
                break;
            }

            // create garbage, filling a larger and larger % of
            // free memory on each loop
            byte[][] bytez = new byte[10000][];
            long avail = rt.freeMemory();
            int create = (int) (avail / 1000 * (950 + toAdd));
            String pct = (95 + (toAdd / 10)) + "." + (toAdd - ((toAdd / 10) * 10));
            int bucket = create / 10000;
            log.info("Filling " + pct + "% of free memory. Free memory=" + avail
                    + " Total Memory=" + rt.totalMemory() + " Max Memory=" + rt.maxMemory());

            try {
                for (int j = 0; j < bytez.length; j++) {
                    bytez[j] = new byte[bucket];
                    if (j % 100 == 0 && ref.get() == null) {
                        return;
                    }
                }
            } catch (Throwable t) {
                bytez = null;
                System.gc();
                System.runFinalization();
                log.warn("Caught throwable filling memory: " + t);
                break;
            } finally {
                bytez = null;
                // Sleep a bit to allow CPU to do work like exchange cluster PING responses
                try {
                    Thread.sleep(20);
                } catch (InterruptedException ignored) {
                    log.warn("Interrupted");
                    break;
                }
            }
        }

        try {
            ByteArrayOutputStream byteout = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(byteout);

            out.writeObject(new Dummy());
            out.close();

            ByteArrayInputStream byteInput = new ByteArrayInputStream(byteout.toByteArray());
            ObjectInputStream input = new ObjectInputStream(byteInput);
            input.readObject();
            input.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (ref.get() != null) {
            System.gc();
            System.runFinalization();
        }
    }

   /** Used just to serialize anything and release SoftCache on java Serialization */
   private static class Dummy implements Serializable
   {
        private static final long serialVersionUID = 1L;
   }
    
}
