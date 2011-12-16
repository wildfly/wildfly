
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
package org.jboss.as.test.integration.management.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import org.jboss.as.cli.operation.OperationFormatException;
import org.jboss.as.cli.operation.impl.DefaultOperationRequestBuilder;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.management.util.ModelUtil;
import org.jboss.as.test.integration.management.util.SimpleServlet;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import static org.junit.Assert.*;
import static org.jboss.as.arquillian.container.Authentication.getCallbackHandler;
import static org.jboss.as.test.integration.management.util.ModelUtil.createOpNode;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
public class AbstractMgmtTestBase {

    protected static final int MGMT_PORT = 9999;
    protected static final String tempDir = System.getProperty("java.io.tmpdir");

    private static ModelControllerClient modelControllerClient;
    private static File brokenWar = null;

    protected static void initModelControllerClient(final String hostName, final int port) {
        if (modelControllerClient == null) {
            try {
                modelControllerClient = ModelControllerClient.Factory.create(InetAddress.getByName(hostName), port, getCallbackHandler());
            } catch (UnknownHostException e) {
                throw new RuntimeException("Cannot create model controller client for host: " + hostName + " and port " + port, e);
            }
        }
    }

    protected static void closeModelControllerClient() throws IOException {
        if (modelControllerClient != null) {
            try {
                modelControllerClient.close();
            } finally {
                modelControllerClient = null;
            }
        }
    }

    protected ModelNode executeOperation(final ModelNode op, boolean unwrapResult) throws IOException {
        ModelNode ret = modelControllerClient.execute(op);
        if (! unwrapResult) return ret;

        assertTrue("Management operation " + op.asString() + " failed: " + ret.asString(),
                "success".equals(ret.get("outcome").asString()));
        return ret.get("result");
    }

    protected ModelNode executeOperation(final ModelNode op) throws IOException {
        return executeOperation(op, true);
    }

    protected ModelNode executeOperation(final String address, final String operation) throws IOException {
        return executeOperation(createOpNode(address, operation));
    }

    protected ModelNode executeAndRollbackOperation(final ModelNode op) throws IOException, OperationFormatException {

        ModelNode addDeploymentOp = createOpNode("deployment=malformedDeployment.war", "add");
        addDeploymentOp.get("content").get(0).get("input-stream-index").set(0);
        ModelNode deploymentOp = new ModelNode();

        DefaultOperationRequestBuilder builder = new DefaultOperationRequestBuilder();
        builder.setOperationName("deploy");
        builder.addNode("deployment", "malformedDeployment.war");


        ModelNode[] steps = new ModelNode[3];
        steps[0] = op;
        steps[1] = addDeploymentOp;
        steps[2] = builder.buildRequest();
        ModelNode compositeOp = ModelUtil.createCompositeNode(steps);

        OperationBuilder ob = new OperationBuilder(compositeOp, true);
        ob.addInputStream(new FileInputStream(getBrokenWar()));

        return modelControllerClient.execute(ob.build());
    }

    private static File getBrokenWar() {
        if (brokenWar != null) return brokenWar;

        WebArchive war = ShrinkWrap.create(WebArchive.class, "deployment2.war");
        war.addClass(SimpleServlet.class);
        war.addAsWebInfResource(new StringAsset("Malformed"), "web.xml");
        brokenWar = new File(System.getProperty("java.io.tmpdir") + File.separator + "malformedDeployment.war");
        brokenWar.deleteOnExit();
        new ZipExporterImpl(war).exportTo(brokenWar, true);
        return brokenWar;
    }
}
