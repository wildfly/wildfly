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
package org.jboss.test.as.protocol.test.base;



/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public interface ServerManagerTest extends BaseTest{

    void testStartServerManagerNoConfig() throws Exception;

    void testStartStopServerManager() throws Exception;

    void testServerStartFailedAndGetsRespawned() throws Exception;

    void testServerStartFailedAndRespawnExpires() throws Exception;

    void testServerCrashedAfterStartGetsRespawned() throws Exception;

    void testRespawnCrashedServerDelayBetweenClosedStreamAndProcessEnded() throws Exception;

    void testServersGetReconnectMessageFollowingRestartedServerManager_StartingDoesNotGetStarted() throws Exception;

    void testServersGetReconnectMessageFollowingRestartedServerManager_StartedDoesNotGetStarted() throws Exception;

    void testServersGetReconnectMessageFollowingRestartedServerManager_StoppingDoesNotGetStarted() throws Exception;

    void testServersGetReconnectMessageFollowingRestartedServerManager_StoppedDoesNotGetStarted() throws Exception;

    void testServersGetReconnectMessageFollowingRestartedServerManager_BootingGetsStarted() throws Exception;

    void testServersGetReconnectMessageFollowingRestartedServerManager_AvailableGetsStarted() throws Exception;

    void testServersGetReconnectMessageFollowingRestartedServerManager_FailedGetsStarted() throws Exception;

    void testServerGetsReconnectedFollowingBrokenConnection() throws Exception;

    void testServerCrashedWhileServerManagerIsDownGetsRestarted() throws Exception;
}
