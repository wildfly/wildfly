/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.deployment.processors;

import org.jboss.as.controller.PathElement;
import org.jboss.as.ee.component.EEApplicationClasses;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.stateless.StatelessComponentDescription;
import org.jboss.as.ejb3.deployment.EjbJarDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentMarker;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.AttachmentList;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.Services;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.dmr.ModelNode;
import org.jboss.jandex.Indexer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.junit.Test;

import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jboss.as.ejb3.TestHelper.index;
import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class TransactionManagementAnnotationProcessorTestCase {
    @TransactionManagement(TransactionManagementType.BEAN)
    private static class MyBean {

    }

    private static class SubBean extends MyBean {

    }

    @Test
    public void test1() throws Exception {
        DeploymentUnit deploymentUnit = this.getDeploymentUnit("test1 Dummy DU");
        // Mark the deployment unit as a EJB deployment
        EjbDeploymentMarker.mark(deploymentUnit);
        DeploymentPhaseContext phaseContext = null;
        Indexer indexer = new Indexer();
        index(indexer, MyBean.class);
        index(indexer, SubBean.class);
        CompositeIndex index = new CompositeIndex(Arrays.asList(indexer.complete()));


        final EEModuleDescription moduleDescription = new EEModuleDescription("TestApp", "TestModule");
        final EEApplicationClasses applicationClassesDescription = new EEApplicationClasses();
        final ServiceName duServiceName = deploymentUnit.getServiceName();
        final EjbJarDescription ejbJarDescription = new EjbJarDescription(moduleDescription, applicationClassesDescription, false);
        EJBComponentDescription componentDescription = new StatelessComponentDescription(MyBean.class.getSimpleName(), MyBean.class.getName(), ejbJarDescription, duServiceName);
        TransactionManagementAnnotationProcessor processor = new TransactionManagementAnnotationProcessor();
        processor.processComponentConfig(deploymentUnit, phaseContext, index, componentDescription);

        assertEquals(TransactionManagementType.BEAN, componentDescription.getTransactionManagementType());
    }

    /**
     * EJB 3.1 FR 13.3.1, the default transaction management type is container-managed transaction demarcation.
     */
    @Test
    public void testDefault() {

        final EEModuleDescription moduleDescription = new EEModuleDescription("TestApp", "TestModule");
        final EEApplicationClasses applicationClassesDescription = new EEApplicationClasses();
        final EjbJarDescription ejbJarDescription = new EjbJarDescription(moduleDescription, applicationClassesDescription, false);
        final ServiceName duServiceName = Services.deploymentUnitName("Dummy deployment unit");
        EJBComponentDescription componentDescription = new StatelessComponentDescription("TestBean", "TestClass", ejbJarDescription, duServiceName);
        assertEquals(TransactionManagementType.CONTAINER, componentDescription.getTransactionManagementType());
    }

    /**
     * EJB 3.1 FR 13.3.6 The TransactionManagement annotation is applied to the enterprise bean class.
     */
    @Test
    public void testSubClass() throws Exception {
        DeploymentUnit deploymentUnit = this.getDeploymentUnit("testSubClass dummy DU");
        // Mark the deployment unit as a EJB deployment
        EjbDeploymentMarker.mark(deploymentUnit);
        DeploymentPhaseContext phaseContext = null;
        Indexer indexer = new Indexer();
        index(indexer, MyBean.class);
        index(indexer, SubBean.class);
        CompositeIndex index = new CompositeIndex(Arrays.asList(indexer.complete()));


        final EEModuleDescription moduleDescription = new EEModuleDescription("TestApp", "TestModule");
        final EEApplicationClasses applicationClassesDescription = new EEApplicationClasses();
        final ServiceName duServiceName = deploymentUnit.getServiceName();
        final EjbJarDescription ejbJarDescription = new EjbJarDescription(moduleDescription, applicationClassesDescription, false);
        EJBComponentDescription componentDescription = new StatelessComponentDescription(SubBean.class.getSimpleName(), SubBean.class.getName(), ejbJarDescription, duServiceName);
        TransactionManagementAnnotationProcessor processor = new TransactionManagementAnnotationProcessor();
        processor.processComponentConfig(deploymentUnit, phaseContext, index, componentDescription);

        assertEquals(TransactionManagementType.CONTAINER, componentDescription.getTransactionManagementType());
    }

    private DeploymentUnit getDeploymentUnit(final String duName) {
        return new DeploymentUnit() {
            private Map<AttachmentKey<?>, Object> attachments = new HashMap();

            @Override
            public ServiceName getServiceName() {
                return Services.deploymentUnitName(duName);
            }

            @Override
            public DeploymentUnit getParent() {
                return null;
            }

            @Override
            public String getName() {
                return duName;
            }

            @Override
            public ServiceRegistry getServiceRegistry() {
                throw new RuntimeException("NYI");
            }

            @Override
            public boolean hasAttachment(AttachmentKey<?> key) {
                return this.attachments.containsKey(key);
            }

            @Override
            public <T> T getAttachment(AttachmentKey<T> key) {
                if (key == null) {
                    return null;
                }
                return key.cast(attachments.get(key));
            }

            @Override
            public <T> List<T> getAttachmentList(AttachmentKey<? extends List<T>> key) {
                throw new RuntimeException("NYI");
            }

            @Override
            public <T> T putAttachment(AttachmentKey<T> key, T value) {
                if (key == null) {
                    throw new IllegalArgumentException("key is null");
                }
                return key.cast(attachments.put(key, key.cast(value)));
            }

            @Override
            public <T> T removeAttachment(AttachmentKey<T> key) {
                throw new RuntimeException("NYI");
            }

            @Override
            public <T> void addToAttachmentList(AttachmentKey<AttachmentList<T>> key, T value) {
                throw new RuntimeException("NYI");
            }

            @Override
            public ModelNode createDeploymentSubModel(String subsystemName, PathElement address) {
                return new ModelNode();
            }
        };

    }
}
