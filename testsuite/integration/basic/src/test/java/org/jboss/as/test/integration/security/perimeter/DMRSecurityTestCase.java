/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.security.perimeter;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.integration.security.common.Utils;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.net.InetAddress;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.security.Constants.SECURITY_DOMAIN;

/**
 * This class contains a check that the management api access is secured.
 *
 * @author <a href="mailto:jlanik@redhat.com">Jan Lanik</a>.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class DMRSecurityTestCase {

    private static final String JBOSS_INST = System.getProperty("jboss.inst");

    private static File originalTokenDir = new File(JBOSS_INST, "/standalone/tmp/auth");
    private static File renamedTokenDir = new File(JBOSS_INST, "/standalone/tmp/auth.renamed");

    /**
     * Workaround to disable silent login on localhost.
     */
    @BeforeClass
    public static void renameTokenDir() {
        originalTokenDir.renameTo(renamedTokenDir);
    }

    /**
     * Enables silent login after the test is completed.
     */
    @AfterClass
    public static void cleanup() {
        renamedTokenDir.renameTo(originalTokenDir);
    }

    /**
     * This test checks that CLI access is secured.
     *
     * @throws Exception We do not provide any credentials so the IOException is required to be thrown.
     */
    @Test(expected = java.io.IOException.class)
    public void testConnect() throws Exception {
        ModelControllerClient modelControllerClient = ModelControllerClient.Factory.create(InetAddress.
           getByName(TestSuiteEnvironment.getServerAddress()), TestSuiteEnvironment.getServerPort());

        ModelNode op = new ModelNode();

        op.get(OP).set(COMPOSITE);
        op.get(OP_ADDR).setEmptyList();
        ModelNode modelNode = op.get(STEPS).add();

        modelNode.get(OP).set(ADD);
        modelNode.get(OP_ADDR).add(SUBSYSTEM, "security");
        modelNode.get(OP_ADDR).add(SECURITY_DOMAIN, "NewSecurityDomain");

        Utils.applyUpdate(op, modelControllerClient);
    }

}
