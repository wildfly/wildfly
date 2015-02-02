/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.web.response;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;

import org.jboss.as.test.integration.management.base.ContainerResourceMgmtTestBase;
import org.jboss.dmr.ModelNode;

/**
 * @author baranowb
 *
 */
public class TestBase extends ContainerResourceMgmtTestBase{

    protected void reload() throws Exception {
        ModelNode operation = new ModelNode();
        operation.get(OP).set("reload");
        executeOperation(operation);
        boolean reloaded = false;
        int i = 0;
        while (!reloaded) {
            try {
                Thread.sleep(5000);
                if (getManagementClient().isServerInRunningState())
                    reloaded = true;
            } catch (Throwable t) {
                // nothing to do, just waiting
            } finally {
                if (!reloaded && i++ > 10)
                    throw new Exception("Server reloading failed");
            }
        }
    }
}
