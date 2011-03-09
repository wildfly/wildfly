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

import org.jboss.as.ejb3.component.EJBComponentDescription;
import org.jboss.as.ejb3.component.MethodIntf;
import org.jboss.as.ejb3.component.stateless.StatelessComponentDescription;
import org.jboss.as.ejb3.deployment.EjbDeploymentMarker;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.jandex.Indexer;
import org.junit.Test;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import java.util.Arrays;
import java.util.Collections;

import static org.jboss.as.ejb3.TestHelper.index;
import static org.jboss.as.ejb3.TestHelper.mockDeploymentUnit;
import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class TransactionAttributeAnnotationProcessorTestCase {
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    private static class MyBean {

    }

    @Test
    public void test1() throws Exception {
        DeploymentUnit deploymentUnit = mockDeploymentUnit();
        // Mark the deployment unit as a EJB deployment
        EjbDeploymentMarker.mark(deploymentUnit);
        DeploymentPhaseContext phaseContext = null;
        Indexer indexer = new Indexer();
        index(indexer, MyBean.class);
        CompositeIndex index = new CompositeIndex(Arrays.asList(indexer.complete()));

        EJBComponentDescription componentDescription = new StatelessComponentDescription(MyBean.class.getSimpleName(), MyBean.class.getName(), "TestModule", "TestApp");
        TransactionAttributeAnnotationProcessor processor = new TransactionAttributeAnnotationProcessor();
        processor.processComponentConfig(deploymentUnit, phaseContext, index, componentDescription);

        assertEquals(TransactionAttributeType.MANDATORY, componentDescription.getTransactionAttribute(MethodIntf.LOCAL, "anyMethod"));
    }
}
