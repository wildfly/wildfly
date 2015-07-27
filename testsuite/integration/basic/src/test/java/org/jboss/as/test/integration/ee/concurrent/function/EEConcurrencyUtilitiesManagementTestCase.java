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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.enterprise.concurrent.ContextService;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.concurrent.ManagedScheduledExecutorService;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.naming.InitialContext;

import org.glassfish.enterprise.concurrent.AbstractManagedExecutorService.RejectPolicy;
import org.glassfish.enterprise.concurrent.ContextServiceImpl;
import org.glassfish.enterprise.concurrent.ManagedThreadFactoryImpl;
import org.glassfish.enterprise.concurrent.internal.ManagedScheduledThreadPoolExecutor;
import org.glassfish.enterprise.concurrent.internal.ManagedThreadPoolExecutor;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.ee.concurrent.ManagedExecutorServiceImpl;
import org.jboss.as.ee.concurrent.ManagedScheduledExecutorServiceImpl;
import org.jboss.as.ee.subsystem.ContextServiceResourceDefinition;
import org.jboss.as.ee.subsystem.EESubsystemModel;
import org.jboss.as.ee.subsystem.ManagedExecutorServiceResourceDefinition;
import org.jboss.as.ee.subsystem.ManagedScheduledExecutorServiceResourceDefinition;
import org.jboss.as.ee.subsystem.ManagedThreadFactoryResourceDefinition;
import org.jboss.as.test.integration.management.ManagementOperations;
import org.jboss.as.test.integration.management.util.MgmtOperationException;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test case for adding and removing EE concurrency utilities. Validate properties of created resources.
 * Set same invalid properties and validate them.
 *
 * @author Hynek Svabek
 */
@RunWith(Arquillian.class)
public class EEConcurrencyUtilitiesManagementTestCase extends AbstractEEConcurrencyUtilitiesTestCase {

    private static final String RESOURCE_NAME = EEConcurrencyUtilitiesManagementTestCase.class.getSimpleName();

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(JavaArchive.class, EEConcurrencyUtilitiesManagementTestCase.class.getSimpleName() + ".jar")
                .addClasses(EEConcurrencyUtilitiesManagementTestCase.class
                        , ManagementOperations.class, MgmtOperationException.class, AbstractEEConcurrencyUtilitiesTestCase.class)
                .addAsManifestResource(new StringAsset("Dependencies: org.jboss.as.controller, org.jboss.as.ee\n"), "MANIFEST.MF");
    }
    
    @Test
    public void testContextServiceManagement() throws Exception {
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
            // lookup
            ContextService service = (ContextService)new InitialContext().lookup(jndiName);
            // lookup
            Assert.assertNotNull(service);

            //Test for using our settings
            ContextServiceImpl contextServiceIml = (ContextServiceImpl)service;
            assertEquals(useTransactionSetupProvider, contextServiceIml.getTransactionSetupProvider() != null);
        } finally {
            safeResourceRemove(pathAddress, jndiName);
        }
    }
    
    @Test
    public void testContextServiceManagementInvalidProperties() throws Exception {
        final PathAddress pathAddress = EE_SUBSYSTEM_PATH_ADDRESS.append(EESubsystemModel.CONTEXT_SERVICE, RESOURCE_NAME);
        final String jndiName = "java:jboss/ee/concurrency/contextservice/"+RESOURCE_NAME;

        try{
            //try to add with wrong attributes
            ModelNode addOperation = Util.createAddOperation(pathAddress);
            addOperation.get(ContextServiceResourceDefinition.JNDI_NAME).set(jndiName);
            addOperation.get(ContextServiceResourceDefinition.USE_TRANSACTION_SETUP_PROVIDER).set("wrongDataType");
            ModelNode addResult = managementClient.getControllerClient().execute(addOperation);
            Assert.assertTrue(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());
        }finally{
            //try to remove... only for to be sure
            removeResource(pathAddress, jndiName);
        }
    }

    @Test
    public void testManagedThreadFactoryManagement() throws Exception {
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
            // lookup
            ManagedThreadFactory service = (ManagedThreadFactory)new InitialContext().lookup(jndiName);
            // lookup
            Assert.assertNotNull(service);

            //Test for using our settings
            ManagedThreadFactoryImpl serviceImpl = (ManagedThreadFactoryImpl)service;
            //ManageThreadFactoryService add prefix "EE-ManagedThreadFactory-"
            Assert.assertTrue(serviceImpl.getName(), serviceImpl.getName().matches(".*-"+RESOURCE_NAME));
            Object priorityValue = getValue(Number.class, service, "priority");
            assertEquals(priority, priorityValue);
            ContextServiceImpl contextServiceIml = getValue(ContextServiceImpl.class, service, "contextService");
            assertEquals(contextServiceName, contextServiceIml.getName());
        } finally {
            safeResourceRemove(pathAddress, jndiName);
        }
    }
    
    @Test
    public void testManagedThreadFactoryManagementInvalidProperties() throws Exception {
        final PathAddress pathAddress = EE_SUBSYSTEM_PATH_ADDRESS.append(EESubsystemModel.MANAGED_THREAD_FACTORY, RESOURCE_NAME);
        final String jndiName = "java:jboss/ee/concurrency/threadfactory/"+RESOURCE_NAME;

        try{
            //try to add with wrong attributes
            ModelNode addOperation = Util.createAddOperation(pathAddress);
            addOperation.get(ManagedThreadFactoryResourceDefinition.JNDI_NAME).set(jndiName);
            addOperation.get(ManagedThreadFactoryResourceDefinition.CONTEXT_SERVICE).set("nonExistContextServiceName");
            ModelNode addResult = managementClient.getControllerClient().execute(addOperation);
            Assert.assertTrue(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());

            addOperation = Util.createAddOperation(pathAddress);
            addOperation.get(ManagedThreadFactoryResourceDefinition.JNDI_NAME).set(jndiName);
            addOperation.get(ManagedThreadFactoryResourceDefinition.PRIORITY).set(Thread.MAX_PRIORITY+1);
            addResult = managementClient.getControllerClient().execute(addOperation);
            Assert.assertTrue(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());

            addOperation = Util.createAddOperation(pathAddress);
            addOperation.get(ManagedThreadFactoryResourceDefinition.JNDI_NAME).set(jndiName);
            addOperation.get(ManagedThreadFactoryResourceDefinition.PRIORITY).set(Thread.MIN_PRIORITY-1);
            addResult = managementClient.getControllerClient().execute(addOperation);
            Assert.assertTrue(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());
        }finally{
            //try to remove... only for to be sure
            removeResource(pathAddress, jndiName);
        }
    }
    
    @Test
    public void testManagedExecutorServiceManagement() throws Exception {
        final PathAddress pathAddress = EE_SUBSYSTEM_PATH_ADDRESS.append(EESubsystemModel.MANAGED_EXECUTOR_SERVICE, RESOURCE_NAME);
        // add
        final String jndiName = "java:jboss/ee/concurrency/executor/"+RESOURCE_NAME;
        
        //params
        //NonDefault values
        final RejectPolicy rejectPolicy = RejectPolicy.RETRY_ABORT;
        final long hungTaskThreshold = 10;
        final String contextServiceName = "default";
        final long keepAliveTime = 40000;
        final long coreThreads = 2;
        final int maxThreads = 5;
        final int queueLength = 10;
        final boolean longRunningTasks = true;
                
        final ModelNode addOperation = Util.createAddOperation(pathAddress);
        addOperation.get(ManagedExecutorServiceResourceDefinition.JNDI_NAME).set(jndiName);
        addOperation.get(ManagedExecutorServiceResourceDefinition.CORE_THREADS).set(coreThreads);//mandatory
//        addOperation.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        addOperation.get(ManagedExecutorServiceResourceDefinition.CONTEXT_SERVICE).set(contextServiceName);//default null
        addOperation.get(ManagedExecutorServiceResourceDefinition.REJECT_POLICY).set(rejectPolicy.toString());//default ABORT
        addOperation.get(ManagedExecutorServiceResourceDefinition.HUNG_TASK_THRESHOLD).set(hungTaskThreshold);//default 0
        addOperation.get(ManagedExecutorServiceResourceDefinition.KEEPALIVE_TIME).set(keepAliveTime);//default 60 000
        addOperation.get(ManagedExecutorServiceResourceDefinition.MAX_THREADS).set(maxThreads);//default  Integer.MAX_VALUE
        addOperation.get(ManagedExecutorServiceResourceDefinition.QUEUE_LENGTH).set(queueLength);//default 0 (unlimited)
        addOperation.get(ManagedExecutorServiceResourceDefinition.LONG_RUNNING_TASKS).set(longRunningTasks);//default false

        final ModelNode addResult = managementClient.getControllerClient().execute(addOperation);
        Assert.assertFalse(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());
        
        try {
            ManagedExecutorService service = (ManagedExecutorService)new InitialContext().lookup(jndiName);
            // lookup
            Assert.assertNotNull(service);
            
            //Test for using our settings
            ManagedExecutorServiceImpl serviceImpl = getValue(ManagedExecutorServiceImpl.class, service, "executor");
            assertEquals(rejectPolicy, serviceImpl.getRejectPolicy());
            Assert.assertEquals(RESOURCE_NAME, serviceImpl.getName());
            ContextServiceImpl contextServiceIml = (ContextServiceImpl)serviceImpl.getContextService();
            assertEquals(contextServiceName, contextServiceIml.getName());
            assertEquals(hungTaskThreshold, serviceImpl.getManagedThreadFactory().getHungTaskThreshold());
            
            ManagedThreadPoolExecutor threadPoolExecutor = getValue(ManagedThreadPoolExecutor.class, serviceImpl, "threadPoolExecutor");
            Assert.assertNotNull(threadPoolExecutor);
            assertEquals(coreThreads, threadPoolExecutor.getCorePoolSize());
            assertEquals(maxThreads, threadPoolExecutor.getMaximumPoolSize());
            assertEquals(keepAliveTime, threadPoolExecutor.getKeepAliveTime(TimeUnit.MILLISECONDS));
            
            BlockingQueue<Runnable> workQueue = getValue(BlockingQueue.class, threadPoolExecutor, "workQueue");
            
            int count = 0;
            try {
                while(count < 2*queueLength){
                    workQueue.add(new Runnable() {@Override public void run() {}});
                    count++;
                }
            } catch (IllegalStateException e) {
                //expected
            }
            assertEquals("Queue has different capacity than expected.", queueLength, count);  
        } finally {
            safeResourceRemove(pathAddress, jndiName);
        }
    }
    
    @Test
    public void testManagedExecutorServiceManagementInvalidProperties() throws Exception {
        final PathAddress pathAddress = EE_SUBSYSTEM_PATH_ADDRESS.append(EESubsystemModel.MANAGED_EXECUTOR_SERVICE, RESOURCE_NAME);
        final String jndiName = "java:jboss/ee/concurrency/executor/"+RESOURCE_NAME;
        
        //valid values
        final long coreThreads = 2;
        //invalid values
        final long invalidCoreThreads = -1;
        final String contextServiceName = "nonExistContextServiceName";
        final String rejectPolicy = "cancel this";
        final String hungTaskThreshold = "wrong";
        final int keepAliveTime = -1;
        final int maxThreads = -5;
        final int queueLength = -10;
        final String longRunningTasks = "wrong";
        
        try{
            //try to add with wrong attributes
            ModelNode addOperation = Util.createAddOperation(pathAddress);
            addOperation.get(ManagedExecutorServiceResourceDefinition.JNDI_NAME).set(jndiName);
            addOperation.get(ManagedExecutorServiceResourceDefinition.CORE_THREADS).set(invalidCoreThreads);//mandatory
            ModelNode addResult = managementClient.getControllerClient().execute(addOperation);
            Assert.assertTrue(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());

            addOperation = Util.createAddOperation(pathAddress);
            addOperation.get(ManagedExecutorServiceResourceDefinition.JNDI_NAME).set(jndiName);
            addOperation.get(ManagedExecutorServiceResourceDefinition.CORE_THREADS).set(coreThreads);//mandatory
            addOperation.get(ManagedExecutorServiceResourceDefinition.CONTEXT_SERVICE).set(contextServiceName);//default null
            addResult = managementClient.getControllerClient().execute(addOperation);
            Assert.assertTrue(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());

            addOperation = Util.createAddOperation(pathAddress);
            addOperation.get(ManagedExecutorServiceResourceDefinition.JNDI_NAME).set(jndiName);
            addOperation.get(ManagedExecutorServiceResourceDefinition.CORE_THREADS).set(coreThreads);//mandatory
            addOperation.get(ManagedExecutorServiceResourceDefinition.REJECT_POLICY).set(rejectPolicy);//default ABORT
            addResult = managementClient.getControllerClient().execute(addOperation);
            Assert.assertTrue(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());

            addOperation = Util.createAddOperation(pathAddress);
            addOperation.get(ManagedExecutorServiceResourceDefinition.JNDI_NAME).set(jndiName);
            addOperation.get(ManagedExecutorServiceResourceDefinition.CORE_THREADS).set(coreThreads);//mandatory
            addOperation.get(ManagedExecutorServiceResourceDefinition.HUNG_TASK_THRESHOLD).set(hungTaskThreshold);//default 0
            addResult = managementClient.getControllerClient().execute(addOperation);
            Assert.assertTrue(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());

            addOperation = Util.createAddOperation(pathAddress);
            addOperation.get(ManagedExecutorServiceResourceDefinition.JNDI_NAME).set(jndiName);
            addOperation.get(ManagedExecutorServiceResourceDefinition.CORE_THREADS).set(coreThreads);//mandatory
            addOperation.get(ManagedExecutorServiceResourceDefinition.KEEPALIVE_TIME).set(keepAliveTime);//default 60 000
            addResult = managementClient.getControllerClient().execute(addOperation);
            Assert.assertTrue(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());

            addOperation = Util.createAddOperation(pathAddress);
            addOperation.get(ManagedExecutorServiceResourceDefinition.JNDI_NAME).set(jndiName);
            addOperation.get(ManagedExecutorServiceResourceDefinition.CORE_THREADS).set(coreThreads);//mandatory
            addOperation.get(ManagedExecutorServiceResourceDefinition.MAX_THREADS).set(maxThreads);//default  Integer.MAX_VALUE
            addResult = managementClient.getControllerClient().execute(addOperation);
            Assert.assertTrue(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());

            addOperation = Util.createAddOperation(pathAddress);
            addOperation.get(ManagedExecutorServiceResourceDefinition.JNDI_NAME).set(jndiName);
            addOperation.get(ManagedExecutorServiceResourceDefinition.CORE_THREADS).set(coreThreads);//mandatory
            addOperation.get(ManagedExecutorServiceResourceDefinition.QUEUE_LENGTH).set(queueLength);//default 0 (unlimited)
            addResult = managementClient.getControllerClient().execute(addOperation);
            Assert.assertTrue(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());
            
            addOperation = Util.createAddOperation(pathAddress);
            addOperation.get(ManagedExecutorServiceResourceDefinition.JNDI_NAME).set(jndiName);
            addOperation.get(ManagedExecutorServiceResourceDefinition.CORE_THREADS).set(coreThreads);//mandatory
            addOperation.get(ManagedExecutorServiceResourceDefinition.LONG_RUNNING_TASKS).set(longRunningTasks);//default false
            addResult = managementClient.getControllerClient().execute(addOperation);
            Assert.assertTrue(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());
        }finally{
         //try to remove... only for to be sure
         removeResource(pathAddress, jndiName);
        }
    }
    
    @Test
    public void testManagedScheduledExecutorServiceManagement() throws Exception {
        final PathAddress pathAddress = EE_SUBSYSTEM_PATH_ADDRESS.append(EESubsystemModel.MANAGED_SCHEDULED_EXECUTOR_SERVICE, RESOURCE_NAME);
        // add
        final String jndiName = "java:jboss/ee/concurrency/scheduledexecutor/"+RESOURCE_NAME;
        
        //params
        //NonDefault values
        final RejectPolicy rejectPolicy = RejectPolicy.RETRY_ABORT;
        final long hungTaskThreshold = 10;
        final String contextService = "default";
        final long keepAliveTime = 40000;
        final long coreThreads = 2;
        final boolean longRunningTasks = true;
        
        
        final ModelNode addOperation = Util.createAddOperation(pathAddress);
        addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.JNDI_NAME).set(jndiName);
        addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.CORE_THREADS).set(coreThreads);//mandatory
//        addOperation.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.CONTEXT_SERVICE).set(contextService);//default null
        addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.REJECT_POLICY).set(rejectPolicy.toString());//default ABORT
        addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.HUNG_TASK_THRESHOLD).set(hungTaskThreshold);//default 0
        addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.KEEPALIVE_TIME).set(keepAliveTime);//default 60 000
        addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.LONG_RUNNING_TASKS).set(longRunningTasks);//default false
        
        final ModelNode addResult = managementClient.getControllerClient().execute(addOperation);
        Assert.assertFalse(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());
        
        try {
            ManagedScheduledExecutorService service = (ManagedScheduledExecutorService)new InitialContext().lookup(jndiName);
            // lookup
            Assert.assertNotNull(service);
            
            //Test for using our settings
            ManagedScheduledExecutorServiceImpl serviceImpl = getValue(ManagedScheduledExecutorServiceImpl.class,service, "executor");
            assertEquals(rejectPolicy, serviceImpl.getRejectPolicy());
            Assert.assertEquals(RESOURCE_NAME, serviceImpl.getName());
            ContextServiceImpl contextServiceIml = (ContextServiceImpl)serviceImpl.getContextService();
            assertEquals(contextService, contextServiceIml.getName());
            assertEquals(hungTaskThreshold, serviceImpl.getManagedThreadFactory().getHungTaskThreshold());
            ManagedScheduledThreadPoolExecutor threadPoolExecutor = getValue(ManagedScheduledThreadPoolExecutor.class, serviceImpl, "threadPoolExecutor");
            Assert.assertNotNull(threadPoolExecutor);
            assertEquals(coreThreads, threadPoolExecutor.getCorePoolSize());
            assertEquals(keepAliveTime, threadPoolExecutor.getKeepAliveTime(TimeUnit.MILLISECONDS));
        } finally {
            safeResourceRemove(pathAddress, jndiName);
        }
    }
    
    @Test
    public void testManagedScheduledExecutorServiceManagementInvalidProperties() throws Exception {
        final PathAddress pathAddress = EE_SUBSYSTEM_PATH_ADDRESS.append(EESubsystemModel.MANAGED_EXECUTOR_SERVICE, RESOURCE_NAME);
        final String jndiName = "java:jboss/ee/concurrency/executor/"+RESOURCE_NAME;
        
        //valid values
        final long coreThreads = 2;
        //invalid values
        final long invalidCoreThreads = -1;
        final String contextServiceName = "nonExistContextServiceName";
        final String rejectPolicy = "cancel this";
        final String hungTaskThreshold = "wrong";
        final int keepAliveTime = -1;
        final String longRunningTasks = "wrong";
        
        try{
            //try to add with wrong attributes
            ModelNode addOperation = Util.createAddOperation(pathAddress);
            addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.JNDI_NAME).set(jndiName);
            addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.CORE_THREADS).set(invalidCoreThreads);//mandatory
            ModelNode addResult = managementClient.getControllerClient().execute(addOperation);
            Assert.assertTrue(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());

            addOperation = Util.createAddOperation(pathAddress);
            addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.JNDI_NAME).set(jndiName);
            addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.CORE_THREADS).set(coreThreads);//mandatory
            addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.CONTEXT_SERVICE).set(contextServiceName);//default null
            addResult = managementClient.getControllerClient().execute(addOperation);
            Assert.assertTrue(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());

            addOperation = Util.createAddOperation(pathAddress);
            addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.JNDI_NAME).set(jndiName);
            addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.CORE_THREADS).set(coreThreads);//mandatory
            addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.REJECT_POLICY).set(rejectPolicy);//default ABORT
            addResult = managementClient.getControllerClient().execute(addOperation);
            Assert.assertTrue(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());

            addOperation = Util.createAddOperation(pathAddress);
            addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.JNDI_NAME).set(jndiName);
            addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.CORE_THREADS).set(coreThreads);//mandatory
            addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.HUNG_TASK_THRESHOLD).set(hungTaskThreshold);//default 0
            addResult = managementClient.getControllerClient().execute(addOperation);
            Assert.assertTrue(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());

            addOperation = Util.createAddOperation(pathAddress);
            addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.JNDI_NAME).set(jndiName);
            addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.CORE_THREADS).set(coreThreads);//mandatory
            addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.KEEPALIVE_TIME).set(keepAliveTime);//default 60 000
            addResult = managementClient.getControllerClient().execute(addOperation);
            Assert.assertTrue(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());
            
            addOperation = Util.createAddOperation(pathAddress);
            addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.JNDI_NAME).set(jndiName);
            addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.CORE_THREADS).set(coreThreads);//mandatory
            addOperation.get(ManagedScheduledExecutorServiceResourceDefinition.LONG_RUNNING_TASKS).set(longRunningTasks);//default false
            addResult = managementClient.getControllerClient().execute(addOperation);
            Assert.assertTrue(addResult.get(FAILURE_DESCRIPTION).toString(), addResult.get(FAILURE_DESCRIPTION).isDefined());
        }finally{
         //try to remove... only for to be sure
         removeResource(pathAddress, jndiName);
        }
    }
}
