/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.remoting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.jboss.as.controller.services.path.AbsolutePathService;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ControllerInitializer;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.remoting3.Endpoint;
import org.junit.Test;
import org.wildfly.extension.io.IOServices;
import org.wildfly.extension.io.WorkerService;
import org.xnio.OptionMap;
import org.xnio.Options;

/**
 * @author Tomaz Cerar
 */
public class RemotingSubsystemTestCase extends AbstractSubsystemBaseTest {

    public RemotingSubsystemTestCase() {
        super(RemotingExtension.SUBSYSTEM_NAME, new RemotingExtension());
    }

    /**
     * Tests that the outbound connections configured in the remoting subsytem are processed and services
     * are created for them
     *
     * @throws Exception
     */
    @Test
    public void testRuntime() throws Exception {
        KernelServices services = createKernelServicesBuilder(createRuntimeAdditionalInitialization())
                .setSubsystemXml(getSubsystemXml())
                .build();

        ServiceController<Endpoint> endPointServiceController = (ServiceController<Endpoint>) services.getContainer().getRequiredService(RemotingServices.SUBSYSTEM_ENDPOINT);
        endPointServiceController.setMode(ServiceController.Mode.ACTIVE);
        Endpoint endpointService = endPointServiceController.getValue();
        assertNotNull("Endpoint service was null", endpointService);
        assertNotNull(endpointService.getName());


        ServiceName connectorServiceName = RemotingServices.serverServiceName("remoting-connector");
        ServiceController<?> remotingConnectorController = services.getContainer().getRequiredService(connectorServiceName);
        remotingConnectorController.setMode(ServiceController.Mode.ACTIVE);
        assertNotNull("Connector was null", remotingConnectorController);
        InjectedSocketBindingStreamServerService remotingConnector = (InjectedSocketBindingStreamServerService) remotingConnectorController.getService();
        assertEquals("remote", remotingConnector.getEndpointInjector().getValue().getName());

        ServiceController<?> httpConnectorController = services.getContainer().getRequiredService(RemotingHttpUpgradeService.UPGRADE_SERVICE_NAME.append("http-connector"));
        assertNotNull("Http connector was null", httpConnectorController);
        httpConnectorController.setMode(ServiceController.Mode.ACTIVE);
        InjectedSocketBindingStreamServerService httpConnector = (InjectedSocketBindingStreamServerService) remotingConnectorController.getService();
        assertEquals("remote", httpConnector.getEndpointInjector().getValue().getName());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("remoting.xml");
    }

    @Override
    protected String getSubsystemXml(String resource) throws IOException {
        return readResource(resource);
    }

    @Override
    protected void compareXml(String configId, String original, String marshalled) throws Exception {
        super.compareXml(configId, original, marshalled, true);
    }

    private AdditionalInitialization createRuntimeAdditionalInitialization() {
        return new AdditionalInitialization() {
            @Override
            protected void setupController(ControllerInitializer controllerInitializer) {
                controllerInitializer.addSocketBinding("remoting", 12345);
                controllerInitializer.addRemoteOutboundSocketBinding("dummy-outbound-socket", "localhost", 6799);
                controllerInitializer.addRemoteOutboundSocketBinding("other-outbound-socket", "localhost", 1234);
            }

            @Override
            protected void addExtraServices(ServiceTarget target) {
                //Needed for initialization of the RealmAuthenticationProviderService
                AbsolutePathService.addService(ServerEnvironment.CONTROLLER_TEMP_DIR, new File("target/temp" + System.currentTimeMillis()).getAbsolutePath(), target);
                target.addService(IOServices.WORKER.append("default"), new WorkerService(OptionMap.builder().set(Options.WORKER_IO_THREADS, 2).getMap()))
                        .setInitialMode(ServiceController.Mode.ACTIVE)
                        .install();
                target.addService(IOServices.WORKER.append("default-remoting"), new WorkerService(OptionMap.builder().set(Options.WORKER_IO_THREADS, 2).getMap()))
                        .setInitialMode(ServiceController.Mode.ACTIVE)
                        .install();
            }
        };
    }


}
