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
package org.jboss.as.server;

import org.jboss.as.controller.ProcessType;
import org.jboss.as.server.ServerEnvironment.LaunchType;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * @author David Bosschaert
 */
public class ServerControllerModelUtilTestCase {
    @Test
    public void testGetProcessType() {
        testGetProcessType(ServerEnvironment.LaunchType.DOMAIN, ProcessType.DOMAIN_SERVER);
        testGetProcessType(ServerEnvironment.LaunchType.EMBEDDED, ProcessType.EMBEDDED_SERVER);
        testGetProcessType(ServerEnvironment.LaunchType.STANDALONE, ProcessType.STANDALONE_SERVER);
    }

    private void testGetProcessType(LaunchType launchType, ProcessType processType) {
        ServerEnvironment se = Mockito.mock(ServerEnvironment.class);
        Mockito.when(se.getLaunchType()).thenReturn(launchType);

        Assert.assertEquals(processType, ServerControllerModelUtil.getProcessType(se));
    }

    @Test
    public void testGetProcessTypeNull() {
        Assert.assertEquals(ProcessType.EMBEDDED_SERVER, ServerControllerModelUtil.getProcessType(null));
    }
}
