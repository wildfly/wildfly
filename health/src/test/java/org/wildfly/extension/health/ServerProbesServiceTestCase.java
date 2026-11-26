/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.health;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.wildfly.extension.health.HealthSubsystemDefinition.SERVER_HEALTH_PROBES_CAPABILITY;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Supplier;

import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.ModelControllerClientFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class ServerProbesServiceTestCase {

    @Test
    public void testProbeIterationIsSafeDuringShutdown() throws Exception {
        ServerProbesService service = createService();
        service.start(mock(StartContext.class));

        Set<ServerProbe> probes = service.getServerProbes();
        Iterator<ServerProbe> iterator = probes.iterator();
        assertTrue(iterator.hasNext());
        iterator.next();

        service.stop(mock(StopContext.class));

        int count = 1;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }

        assertEquals(4, count); // serverstate, suspendstate, deployments, nobooterror
        assertTrue(probes.isEmpty());
    }

    private static ServerProbesService createService() {
        OperationContext context = mock(OperationContext.class, RETURNS_DEEP_STUBS);
        CapabilityServiceBuilder<?> builder = context.getCapabilityServiceTarget()
                .addCapability(SERVER_HEALTH_PROBES_CAPABILITY);
        ModelControllerClientFactory clientFactory = mock(ModelControllerClientFactory.class, RETURNS_DEEP_STUBS);

        doReturn((Supplier<ModelControllerClientFactory>) () -> clientFactory)
                .when(builder).requires(ModelControllerClientFactory.SERVICE_DESCRIPTOR);

        ServerProbesService.install(context);

        ArgumentCaptor<ServerProbesService> captor = ArgumentCaptor.forClass(ServerProbesService.class);
        verify(builder).setInstance(captor.capture());
        return captor.getValue();
    }
}
