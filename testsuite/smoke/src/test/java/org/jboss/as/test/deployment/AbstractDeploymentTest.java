/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.deployment;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.deployment.client.api.DuplicateDeploymentNameException;
import org.jboss.as.deployment.client.api.server.InitialDeploymentPlanBuilder;
import org.jboss.as.deployment.client.api.server.ServerDeploymentManager;
import org.jboss.as.deployment.client.api.server.ServerDeploymentPlanResult;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerStartException;
import org.jboss.as.server.StandaloneServer;
import org.jboss.as.server.mgmt.deployment.ServerDeploymentManagerImpl;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.junit.Before;

/**
 * A AbstractDeploymentTest.
 * 
 * @author <a href="alex@jboss.com">Alexey Loubyansky</a>
 * @version $Revision: 1.1 $
 */
public abstract class AbstractDeploymentTest {

   private URL managedBeansJar = null;
   private ServerDeploymentManager dm;

   public static boolean delete(File path) {
      if( path.exists() ) {
        File[] files = path.listFiles();
        for(int i=0; i<files.length; i++) {
           if(files[i].isDirectory()) {
             delete(files[i]);
           }
           else {
             files[i].delete();
           }
        }
      }
      return( path.delete() );
    }

   @Before
   public void setUp() {
      managedBeansJar = Thread.currentThread().getContextClassLoader().getResource("deployment/managedbeans.jar");
      assertNotNull("managedbeans.jar exists", managedBeansJar);
      File f = new File(managedBeansJar.getFile());
      File[] children = f.listFiles(new FilenameFilter(){
         @Override
         public boolean accept(File dir, String name) {
            return !name.equals("META-INF");
         }});
      for(File child : children)
         delete(child);
      dm = startEmbeddedAS();
   }

   protected void assertDeploy() throws IOException, DuplicateDeploymentNameException {
      
      assertNotNull("Deployment manager has been initialized.", dm);
      InitialDeploymentPlanBuilder builder = dm.newDeploymentPlan();
      builder.add(managedBeansJar).andDeploy();
      Future<ServerDeploymentPlanResult> future = dm.execute(builder.build());
   
      ServerDeploymentPlanResult result = null;
      try {
         result = future.get(2, TimeUnit.SECONDS);
      } catch (CancellationException ce) {
         fail("The plan was cancelled " + ce);
      } catch (ExecutionException ee) {
         fail("The plan execution failed " + ee);
      } catch (InterruptedException ie) {
         fail("The thread was interrupted " + ie);
      } catch (TimeoutException te) {
         fail("Timed out waiting for the deployment to complete " + te);
      }
   
      assertFalse("The task wasn't cancelled", future.isCancelled());
   
      assertNotNull("The result is not null", result);
      //System.out.println(result.getDeploymentPlanId());
      //ServerDeploymentActionResult deploymentActionResult = result.getDeploymentActionResult(result.getDeploymentPlanId());
      //System.out.println(deploymentActionResult);
   }

   protected void addToJar(Class<?> cls) {
      assertNotNull("managedbeans.jar exists", managedBeansJar);
   
      String classDir = cls.getPackage().getName().replace('.', '/');
      URL classDirURL = Thread.currentThread().getContextClassLoader().getResource(classDir);
      assertNotNull("support package is built", classDirURL);
      File classFile = new File(classDirURL.getFile(), cls.getSimpleName() + ".class");
      assertTrue(cls.getName() + " is found in " + classDirURL.getFile(), classFile.exists());
      
      String pkg = cls.getPackage().getName();
      String path = pkg.replace('.', '/');
      File targetFile = new File(managedBeansJar.getFile(), path);
      targetFile.mkdirs();
      targetFile = new File(targetFile, cls.getSimpleName() + ".class");
      //assertFalse(targetFile.getAbsoluteFile() + " doesn't exist", targetFile.exists());
      //targetFile.createNewFile();
      
      FileInputStream fis = null;
      FileOutputStream fos = null;
      try {
         fis = new FileInputStream(classFile);
         fos = new FileOutputStream(targetFile);
         byte[] bytes = new byte[fis.available()];
         int read = fis.read(bytes);
         while(read > 0) {
            fos.write(bytes, 0, read);
            read = fis.read(bytes);
         }
      } catch(Exception e) {
         fail("Failed to copy " + classFile.getAbsolutePath() + " to " + targetFile.getAbsolutePath() + ": " + e);
      } finally {
         if(fis != null)
            try {
               fis.close();
            } catch(IOException e) {}
         if(fos != null)
            try {
               fos.close();
            } catch(IOException e) {}
      }
   
      assertTrue(targetFile.getAbsoluteFile() + " exists", targetFile.exists());
   }

   protected String getASHome() {
      File f = new File(".");
      f = f.getAbsoluteFile();
      while(f.getParentFile() != null) {
         if("testsuite".equals(f.getName())) {
            assertNotNull("Expected to find a parent directory for " + f.getAbsolutePath(), f.getParentFile());
            f = f.getParentFile();
            f = new File(f, "build");
            assertTrue("The server 'build' dir exists", f.exists());
            f = new File(f, "target");
            if(!f.exists())
               fail("The server hasn't been built yet.");
            assertTrue("The server 'build/target' dir exists", f.exists());
            return f.getAbsolutePath();
         } else {
            f = f.getParentFile();
         }
      }
      return null;
   }

   protected ServerDeploymentManager startEmbeddedAS() {
      String asHome = getASHome();
      Properties props = new Properties();
      props.setProperty(ServerEnvironment.HOME_DIR, asHome);
   
      ServerEnvironment env = new ServerEnvironment(props, "embedded as7", true);
      System.setProperty("module.path", env.getModulesDir().getAbsolutePath());
   
      DeploymentManagerProvider service = new DeploymentManagerProvider();
      StandaloneServer server = new StandaloneServer(env);
      try {
         server.start(Collections.<ServiceActivator>singletonList(service));
      } catch (ServerStartException e) {
         e.printStackTrace();
         fail("Failed to start embedded server.");
      }
      
      synchronized(service) {
         try {
            service.wait(5000);
         } catch(InterruptedException e) {
         }
      }
      
      assertTrue("Deployment manager provider started.", service.isStarted());
      ServerDeploymentManager deploymentManager = service.getDeploymentManager();
      assertNotNull(deploymentManager);
      return deploymentManager;
   }

   protected static class DeploymentManagerProvider implements Service<DeploymentManagerProvider>, ServiceActivator {
      
      private final InjectedValue<ServerDeploymentManager> deploymentManager = new InjectedValue<ServerDeploymentManager>();
         
      private boolean started;
   
      public boolean isStarted() {
         return started;
      }
         
      public ServerDeploymentManager getDeploymentManager() {
         return deploymentManager.getValue();
      }
         
      @Override
      public void start(StartContext context) throws StartException {
         started = true;
         synchronized(this) {
            this.notifyAll();
         }
      }
   
      @Override
      public void stop(StopContext context) {
      }
   
      @Override
      public DeploymentManagerProvider getValue() throws IllegalStateException {
         return this;
      }
         
      @Override
      public void activate(ServiceActivatorContext ctx) {
         BatchBuilder builder = ctx.getBatchBuilder();
         builder.addService(ServiceName.of("jboss", "test", "server"), DeploymentManagerProvider.this).addDependency(
               ServerDeploymentManagerImpl.SERVICE_NAME_LOCAL, ServerDeploymentManager.class, deploymentManager);
      }
   }
}