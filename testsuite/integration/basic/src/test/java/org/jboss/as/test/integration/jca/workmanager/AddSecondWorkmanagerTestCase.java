/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.jca.workmanager;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.test.integration.management.base.AbstractCliTestBase;
import org.jboss.as.test.integration.management.util.CLIWrapper;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test tries to add two work managers and checks if the operations passed.
 * Test for [ WFLY-11104 ].
 *
 * @author Daniel Cihak
 */
@RunWith(Arquillian.class)
@RunAsClient
public class AddSecondWorkmanagerTestCase extends AbstractCliTestBase {

    /**
     * Add two work managers into JCA subsystem. This operation should pass.
     */
    @Test
    public void testAddSecondWorkmanager() throws Exception {
        try (CLIWrapper cli = new CLIWrapper(true)) {
            try {
                cli.sendLine("batch");
                cli.sendLine("/subsystem=jca/distributed-workmanager=dwm1:add(name=dwm1)");
                cli.sendLine("/subsystem=jca/distributed-workmanager=dwm1/short-running-threads=dwm1:add(max-threads=11,queue-length=22)");
                cli.sendLine("/subsystem=jca/distributed-workmanager=dwm2:add(name=dwm2)");
                cli.sendLine("/subsystem=jca/distributed-workmanager=dwm2/short-running-threads=dwm2:add(max-threads=11,queue-length=22)");
                cli.sendLine("run-batch");
            } finally {
                cli.sendLine("/subsystem=jca/distributed-workmanager=dwm2:remove");
                cli.sendLine("/subsystem=jca/distributed-workmanager=dwm1:remove");
            }
        }
    }
}
