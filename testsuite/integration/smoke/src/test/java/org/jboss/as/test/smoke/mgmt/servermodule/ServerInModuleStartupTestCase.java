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
package org.jboss.as.test.smoke.mgmt.servermodule;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;


import junit.framework.Assert;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.test.shared.TestUtils;
import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Here to prove the forked surefire plugin is capable of running
 * modular tests. This plugin will load up this test class in a module that can see
 * org.jboss.as.standalone.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
public class ServerInModuleStartupTestCase  {

    /**
     * Validates that the model can be read in xml form.
     *
     * @throws Exception
     */
    @Test
    public void testReadConfigAsXml() throws Exception {
        ModelControllerClient client = TestUtils.getModelControllerClient();
        try {
            ModelNode request = new ModelNode();
            request.get("operation").set("read-config-as-xml");
            request.get("address").setEmptyList();
            ModelNode r = client.execute(request);

            Assert.assertEquals(SUCCESS, r.require(OUTCOME).asString());
        } finally {
            StreamUtils.safeClose(client);
        }
    }

    /**
     * Validates that all resource and operation descriptions can be generated.
     *
     * @throws Exception
     */
    @Test
    public void testReadResourceDescription() throws Exception {
        ModelControllerClient client = TestUtils.getModelControllerClient();
        try {
            ModelNode request = new ModelNode();
            request.get("operation").set("read-resource");
            request.get("address").setEmptyList();
            request.get("recursive").set(true);
            ModelNode r = client.execute(request);

            Assert.assertEquals("response with failure details:"+r.toString(), SUCCESS, r.require(OUTCOME).asString());

            request = new ModelNode();
            request.get("operation").set("read-resource-description");
            request.get("address").setEmptyList();
            request.get("recursive").set(true);
            request.get("operations").set(true);
            request.get("inherited").set(false);
            r = client.execute(request);

            Assert.assertEquals("response with failure details:"+r.toString(), SUCCESS, r.require(OUTCOME).asString());

            // Make sure the inherited op descriptions work as well

            request = new ModelNode();
            request.get("operation").set("read-resource-description");
            request.get("address").setEmptyList();
            request.get("recursive").set(false); // NOT recursive; we just need them once
            request.get("operations").set(true);
            request.get("inherited").set(true);
            r = client.execute(request);

            Assert.assertEquals("response with failure details:"+r.toString(), SUCCESS, r.require(OUTCOME).asString());
        } finally {
            StreamUtils.safeClose(client);
        }
    }

}
