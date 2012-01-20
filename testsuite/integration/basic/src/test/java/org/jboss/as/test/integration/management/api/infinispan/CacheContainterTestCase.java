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
package org.jboss.as.test.integration.management.api.infinispan;

import java.io.IOException;
import java.net.URL;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.management.base.AbstractMgmtTestBase;
import org.jboss.as.test.integration.management.cli.GlobalOpsTestCase;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.runner.RunWith;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.junit.Test;


/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class CacheContainterTestCase extends AbstractMgmtTestBase {

    @ArquillianResource URL url;

    private static final String TEST_CONTAINER = "test-container";
    
    @Deployment
    public static Archive<?> getDeployment() {
        JavaArchive ja = ShrinkWrap.create(JavaArchive.class, "dummy.jar");
        ja.addClass(GlobalOpsTestCase.class);
        return ja;
    }
    
    @Before
    public void before() throws Exception {
        initModelControllerClient(url.getHost(), MGMT_PORT);
    }
    
    @AfterClass
    public static void afterClass() throws IOException {
        closeModelControllerClient();
    }
    
    @Test
    public void testAddAndRemoveCacheContainer() throws Exception {

        // add cache container
        ModelNode op = createOpNode(
                "subsystem=infinispan/cache-container=" + TEST_CONTAINER, ModelDescriptionConstants.ADD);       
        executeOperation(op);        
        
        // check the container is in the model
        op = createOpNode("subsystem=infinispan/cache-container=" + TEST_CONTAINER, ModelDescriptionConstants.READ_RESOURCE_OPERATION);
        executeOperation(op);
        
        // remove cache container
        op = createOpNode("subsystem=infinispan/cache-container=" + TEST_CONTAINER, ModelDescriptionConstants.REMOVE);       
        executeOperation(op);        
        
    }
    
}
