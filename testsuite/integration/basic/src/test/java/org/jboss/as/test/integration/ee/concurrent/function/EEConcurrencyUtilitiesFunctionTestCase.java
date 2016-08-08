/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ee.concurrent.function;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import javax.naming.InitialContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.ee.subsystem.ContextServiceResourceDefinition;
import org.jboss.as.ee.subsystem.EESubsystemModel;
import org.jboss.as.ee.subsystem.ManagedExecutorServiceResourceDefinition;
import org.jboss.as.ee.subsystem.ManagedScheduledExecutorServiceResourceDefinition;
import org.jboss.as.ee.subsystem.ManagedThreadFactoryResourceDefinition;
import org.jboss.as.test.integration.ee.concurrent.TestEJBRunnable;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.security.client.SecurityClient;
import org.jboss.security.client.SecurityClientFactory;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case
 * 
 * Test case for adding and removing EE concurrency utilities. Validate properties of created resources.
 * Set same invalid properties and validate them.
 *
 * @author Hynek Svabek
 */
@RunWith(Arquillian.class)
public class EEConcurrencyUtilitiesFunctionTestCase extends AbstractEEConcurrencyUtilitiesTestCase{

    private static final String RESOURCE_NAME = EEConcurrencyUtilitiesFunctionTestCase.class.getSimpleName();
        
    @Deployment
    public static WebArchive getDeployment() {
        return ShrinkWrap.create(WebArchive.class, EEConcurrencyUtilitiesFunctionTestCase.class.getSimpleName()+".war")
                .addClasses(EEConcurrencyUtilitiesFunctionTestCase.class, TestEJBRunnable.class,
                        ManagementOperations.class, MgmtOperationException.class, AbstractEEConcurrencyUtilitiesTestCase.class,
                        TestDelayEJBRunnable.class,
                        EEConcurrencyUtilitiesFunctionThreadFactoryTestEJB.class,
                        EEConcurrencyUtilitiesManagedExecutorServiceTestEJB.class,
                        EEConcurrencyUtilitiesManagedScheduledExecutorServiceTestEJB.class,
                        EEConcurrencyUtilitiesContextServiceTestEJB.class)  
                .addAsManifestResource(TestEJBRunnable.class.getPackage(), "permissions.xml", "permissions.xml")
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller, org.jboss.as.ee\n"), "MANIFEST.MF");
    }
    
    @Test
    public void testTaskSubmitContextService() throws Throwable { 
      final PathAddress pathAddress = EE_SUBSYSTEM_PATH_ADDRESS.append(EESubsystemModel.CONTEXT_SERVICE, RESOURCE_NAME);
      // add
      final String jndiName = "java:jboss/ee/concurrency/contextservice/"+RESOURCE_NAME;

      //params
      //NonDefault values
      final boolean useTransactionSetupProvider = true;

      final ModelNode addOperation = Util.createAddOperation(pathAddress);
      addOperation.get(ContextServiceResourceDefinition.JNDI_NAME).set(jndiName);
      addOperation.get(ContextServiceResourceDefinition.USE_TRANSACTION_SETUP_PROVIDER).set(useTransactionSetupProvider);//default false

      final ModelNode addResult = managementClient.getControllerClient().execute(addOperation);
      Assert.assertFalse(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());
      try {     
          SecurityClient client = SecurityClientFactory.getSecurityClient();
          client.setSimple("guest", "guest");
          client.login();
          try {
              
              final EEConcurrencyUtilitiesContextServiceTestEJB testEJB = (EEConcurrencyUtilitiesContextServiceTestEJB) new InitialContext().lookup("java:module/" + EEConcurrencyUtilitiesContextServiceTestEJB.class.getSimpleName());
              
              final CountDownLatch latch = new CountDownLatch(1);

              testEJB.submit(jndiName, new TestDelayEJBRunnable(latch));

              //we are waiting for end - There can stuck thread (exception, ....) -> fail
              long timeout = (1000);
              latch.await(timeout, TimeUnit.MILLISECONDS);
              long runningThreadsCount = latch.getCount();
              if(runningThreadsCount > 0){
                  Assert.fail("There are some running threads: [" + runningThreadsCount+"]");
              }
          } catch (Exception e){
              Assert.fail(e.getMessage());
          } 
          finally {
              client.logout();
          }
      } finally {
          safeResourceRemove(pathAddress, jndiName);
      }
    }
    
    @Test
    public void testTaskSubmitWithoutContextService() throws Throwable { 
          SecurityClient client = SecurityClientFactory.getSecurityClient();
          client.setSimple("guest", "guest");
          client.login();
          try {
              
              final EEConcurrencyUtilitiesContextServiceTestEJB testEJB = (EEConcurrencyUtilitiesContextServiceTestEJB) new InitialContext().lookup("java:module/" + EEConcurrencyUtilitiesContextServiceTestEJB.class.getSimpleName());
              
              final CountDownLatch latch = new CountDownLatch(1);

              testEJB.submit(new TestDelayEJBRunnable(latch));

              //we are waiting for end - There can stuck thread (exception, ....) -> fail
              long timeout = (1000);
              latch.await(timeout, TimeUnit.MILLISECONDS);
              long runningThreadsCount = latch.getCount();
              if(runningThreadsCount == 0){
                  Assert.fail("We expect exception in task thread.");
              }
          } catch (Exception e){
              Assert.fail(e.getMessage());
          } 
          finally {
              client.logout();
          }
    }
    
    @Test
    public void testTaskSubmitThreadFactory() throws Exception { 
      final PathAddress pathAddress = EE_SUBSYSTEM_PATH_ADDRESS.append(EESubsystemModel.MANAGED_THREAD_FACTORY, RESOURCE_NAME);
      // add
      final String jndiName = "java:jboss/ee/concurrency/threadfactory/"+RESOURCE_NAME;

      //params
      //NonDefault values
      final String contextServiceName = "default";
      final int priority = 2;

      final ModelNode addOperation = Util.createAddOperation(pathAddress);
      addOperation.get(ManagedThreadFactoryResourceDefinition.JNDI_NAME).set(jndiName);
      addOperation.get(ManagedThreadFactoryResourceDefinition.CONTEXT_SERVICE).set(contextServiceName);//default null
      addOperation.get(ManagedThreadFactoryResourceDefinition.PRIORITY).set(priority);//default 5

      final ModelNode addResult = managementClient.getControllerClient().execute(addOperation);
      Assert.assertFalse(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());
      try {     
          SecurityClient client = SecurityClientFactory.getSecurityClient();
          client.setSimple("guest", "guest");
          client.login();
          try {
              final EEConcurrencyUtilitiesFunctionThreadFactoryTestEJB testEJB = (EEConcurrencyUtilitiesFunctionThreadFactoryTestEJB) new InitialContext().lookup("java:module/" + EEConcurrencyUtilitiesFunctionThreadFactoryTestEJB.class.getSimpleName());
              for(int i = 0; i < 5; i++){
                  testEJB.run(jndiName, new TestEJBRunnable());
              }
          } catch (Exception e){
              Assert.fail(e.getMessage());
          } 
          finally {
              client.logout();
          }
      } finally {
          safeResourceRemove(pathAddress, jndiName);
      }
    }
    
    @Test
    public void testTaskSubmitManagedExecutorService() throws Exception {
        final PathAddress pathAddress = EE_SUBSYSTEM_PATH_ADDRESS.append(EESubsystemModel.MANAGED_EXECUTOR_SERVICE, RESOURCE_NAME);
        // add
        final String jndiName = "java:jboss/ee/concurrency/executor/"+RESOURCE_NAME;
        
        //params
        final long coreThreads = 1;
        final int maxThreads = 3;
        final int queueLength = 5;
                
        final ModelNode addOperation = Util.createAddOperation(pathAddress);
        addOperation.get(ManagedExecutorServiceResourceDefinition.JNDI_NAME).set(jndiName);
        addOperation.get(ManagedExecutorServiceResourceDefinition.CORE_THREADS).set(coreThreads);//mandatory
        addOperation.get(ManagedExecutorServiceResourceDefinition.MAX_THREADS).set(maxThreads);//default  Integer.MAX_VALUE
        addOperation.get(ManagedExecutorServiceResourceDefinition.QUEUE_LENGTH).set(queueLength);//default 0 (unlimited)
        addOperation.get(ManagedExecutorServiceResourceDefinition.CONTEXT_SERVICE).set("default");
//        addOperation.get(ManagedExecutorServiceResourceDefinition.THREAD_FACTORY).set("default");//

        final ModelNode addResult = managementClient.getControllerClient().execute(addOperation);
        Assert.assertFalse(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());
        
        try {
            SecurityClient client = SecurityClientFactory.getSecurityClient();
            client.setSimple("guest", "guest");
            client.login();
            try {
                final EEConcurrencyUtilitiesManagedExecutorServiceTestEJB testEJB = (EEConcurrencyUtilitiesManagedExecutorServiceTestEJB) new InitialContext().lookup("java:module/" + EEConcurrencyUtilitiesManagedExecutorServiceTestEJB.class.getSimpleName());
                
                int max = maxThreads+queueLength;
                long delayMillis = 500;
                final CountDownLatch latch = new CountDownLatch(max);//I set here count only for first FOR-CYCLE, second FOR-CYCLE will crash after start

              //it must be ok
                for(int i=0; i < max; i++){
                    testEJB.submit(jndiName, new TestDelayEJBRunnable(delayMillis, latch));
                }
                boolean expectedException = false;
                try{
                    //i < 1 it would be enough, but it is for sure.
                    for(int i=0; i < (max+1); i++){
                        testEJB.submit(jndiName, new TestDelayEJBRunnable(delayMillis));
                    }
                }catch (Exception e){
                    if(!(e instanceof RejectedExecutionException || e.getCause() instanceof RejectedExecutionException)){
                        throw e;
                    }
                    expectedException = true;
                }
                if(!expectedException){
                    Assert.fail("We expected exception.");
                }
                
                //we are waiting for end - There can stuck some threads -> fail
                long timeout = (max*delayMillis)+(delayMillis);
                latch.await(timeout, TimeUnit.MILLISECONDS);
                long runningThreadsCount = latch.getCount();
                if(runningThreadsCount > 0){
                    Assert.fail("There are some running threads: [" + runningThreadsCount+"]");
                }
            } finally {
                client.logout();
            }
        } finally {
            safeResourceRemove(pathAddress, jndiName);
        }
    }
    
    @Test
    public void testTaskSubmitManagedScheduledExecutorService() throws Exception {
        final PathAddress pathAddress = EE_SUBSYSTEM_PATH_ADDRESS.append(EESubsystemModel.MANAGED_SCHEDULED_EXECUTOR_SERVICE, RESOURCE_NAME);
        // add
        final String jndiName = "java:jboss/ee/concurrency/scheduledexecutor/"+RESOURCE_NAME;
        
        //params
        final long coreThreads = 10;
                
        final ModelNode addOperation = Util.createAddOperation(pathAddress);
        addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.JNDI_NAME).set(jndiName);
        addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.CORE_THREADS).set(coreThreads);//mandatory
        addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.CONTEXT_SERVICE).set("default");
//        addOperation.get(ManagedExecutorServiceResourceDefinition.THREAD_FACTORY).set("default");//

        final ModelNode addResult = managementClient.getControllerClient().execute(addOperation);
        Assert.assertFalse(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());
        
        try {
            SecurityClient client = SecurityClientFactory.getSecurityClient();
            client.setSimple("guest", "guest");
            client.login();
            try {
                final EEConcurrencyUtilitiesManagedScheduledExecutorServiceTestEJB testEJB = (EEConcurrencyUtilitiesManagedScheduledExecutorServiceTestEJB) new InitialContext().lookup("java:module/" + EEConcurrencyUtilitiesManagedScheduledExecutorServiceTestEJB.class.getSimpleName());
                
                int max = 100000;
                long delayMillis = 100;
                final CountDownLatch latch = new CountDownLatch(max);

                for(int i=0; i < max; i++){
                    testEJB.schedule(jndiName, new TestDelayEJBRunnable(latch), delayMillis, TimeUnit.MILLISECONDS);
                }
                
                //we are waiting for the end - There can stuck some threads -> fail
                long timeout = (max*delayMillis/coreThreads);
                latch.await(timeout, TimeUnit.MILLISECONDS);
                if(latch.getCount() != 0){
                    Assert.fail("There are some running threads: [" + latch.getCount()+"]");
                }
            } finally {
                client.logout();
            }
        } finally {
            safeResourceRemove(pathAddress, jndiName);
        }
    }
}
