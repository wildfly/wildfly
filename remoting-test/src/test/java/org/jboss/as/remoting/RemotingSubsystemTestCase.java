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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.fail;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.io.File;
import java.io.IOException;

import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.services.path.AbsolutePathService;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ControllerInitializer;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceNotFoundException;
import org.jboss.msc.service.ServiceTarget;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RemotingSubsystemTestCase extends AbstractSubsystemBaseTest {

    public RemotingSubsystemTestCase() {
        // FIXME RemotingSubsystemTestCase constructor
        super(RemotingExtension.SUBSYSTEM_NAME, new RemotingExtension());
    }

    @Test
    public void testSubsystemWithThreadParameters() throws Exception {
        standardSubsystemTest("remoting-with-threads.xml");
    }

    @Test
    public void testSubsystemWitThreadsAndConnectorProperties() throws Exception {
        final int port = 12345;
        KernelServices services = installInController(new AdditionalInitialization(){
                @Override
                protected void setupController(ControllerInitializer controllerInitializer) {
                    controllerInitializer.addSocketBinding("test", port);
                }

                @Override
                protected void addExtraServices(ServiceTarget target) {
                    //Needed for initialization of the RealmAuthenticationProviderService
                    AbsolutePathService.addService(ServerEnvironment.CONTROLLER_TEMP_DIR, new File("target/temp" + System.currentTimeMillis()).getAbsolutePath(), target);
                }

            },readResource("remoting-with-threads.xml"));

        ServiceController<?> endPointService = services.getContainer().getRequiredService(RemotingServices.SUBSYSTEM_ENDPOINT);
        assertNotNull(endPointService);

        ServiceName connectorServiceName = RemotingServices.serverServiceName("test-connector");
        ServiceController<?> connectorService = services.getContainer().getRequiredService(connectorServiceName);
        assertNotNull(connectorService);

        ModelNode model = services.readWholeModel();
        ModelNode subsystem = model.require(SUBSYSTEM).require(RemotingExtension.SUBSYSTEM_NAME);
        assertEquals(5, subsystem.require(CommonAttributes.WORKER_READ_THREADS).asInt());
        assertEquals(6, subsystem.require(CommonAttributes.WORKER_TASK_CORE_THREADS).asInt());
        assertEquals(7, subsystem.require(CommonAttributes.WORKER_TASK_KEEPALIVE).asInt());
        assertEquals(8, subsystem.require(CommonAttributes.WORKER_TASK_LIMIT).asInt());
        assertEquals(9, subsystem.require(CommonAttributes.WORKER_TASK_MAX_THREADS).asInt());
        assertEquals(10, subsystem.require(CommonAttributes.WORKER_WRITE_THREADS).asInt());

        ModelNode connector = subsystem.require(CommonAttributes.CONNECTOR).require("test-connector");
        assertEquals(1, connector.require(CommonAttributes.PROPERTY).require("org.xnio.Options.WORKER_ACCEPT_THREADS").require(CommonAttributes.VALUE).asInt());
    }

    @Test @Ignore("AS7-2717")
    public void testSubsystemWithThreadAttributeChange() throws Exception {
        final int port = 12345;
        KernelServices services = installInController(new AdditionalInitialization(){
                @Override
                protected void setupController(ControllerInitializer controllerInitializer) {
                    controllerInitializer.addSocketBinding("test", port);
                }

                @Override
                protected void addExtraServices(ServiceTarget target) {
                    //Needed for initialization of the RealmAuthenticationProviderService
                    AbsolutePathService.addService(ServerEnvironment.CONTROLLER_TEMP_DIR, new File("target/temp" + System.currentTimeMillis()).getAbsolutePath(), target);
                }

            },readResource("remoting-with-threads.xml"));

        CurrentConnectorAndController current = CurrentConnectorAndController.create(services, RemotingServices.SUBSYSTEM_ENDPOINT, RemotingServices.serverServiceName("test-connector"));

        updateAndCheckThreadAttribute(services, current, CommonAttributes.WORKER_READ_THREADS, 5, 6);
        updateAndCheckThreadAttribute(services, current, CommonAttributes.WORKER_TASK_CORE_THREADS, 6, 2);
        updateAndCheckThreadAttribute(services, current, CommonAttributes.WORKER_TASK_KEEPALIVE, 7, 3);
        updateAndCheckThreadAttribute(services, current, CommonAttributes.WORKER_TASK_LIMIT, 8, 4);
        updateAndCheckThreadAttribute(services, current, CommonAttributes.WORKER_TASK_MAX_THREADS, 9, 5);
        updateAndCheckThreadAttribute(services, current, CommonAttributes.WORKER_WRITE_THREADS, 10, 6);
    }

    private void updateAndCheckThreadAttribute(KernelServices services, CurrentConnectorAndController current, String attrName, int before, int after) throws Exception {
        assertEquals(before, services.readWholeModel().get(SUBSYSTEM, RemotingExtension.SUBSYSTEM_NAME, attrName).asInt());
        ModelNode write = new ModelNode();
        write.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        write.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        write.get(OP_ADDR).add(SUBSYSTEM, RemotingExtension.SUBSYSTEM_NAME);
        write.get(NAME).set(attrName);
        write.get(VALUE).set(after);
        ModelNode result = services.executeOperation(write);
        assertFalse(result.get(FAILURE_DESCRIPTION).toString(), result.hasDefined(FAILURE_DESCRIPTION));

        assertEquals(after, services.readWholeModel().get(SUBSYSTEM, RemotingExtension.SUBSYSTEM_NAME, attrName).asInt());

        current.updateCurrentEndpoint(false);
        current.updateCurrentConnector(false);
    }


    @Test @Ignore("AS7-2717")
    public void testSubsystemWithConnectorPropertyChange() throws Exception {
        final int port = 12345;
        KernelServices services = installInController(new AdditionalInitialization(){
                @Override
                protected void setupController(ControllerInitializer controllerInitializer) {
                    controllerInitializer.addSocketBinding("test", port);
                }

                @Override
                protected void addExtraServices(ServiceTarget target) {
                    //Needed for initialization of the RealmAuthenticationProviderService
                    AbsolutePathService.addService(ServerEnvironment.CONTROLLER_TEMP_DIR, new File("target/temp" + System.currentTimeMillis()).getAbsolutePath(), target);
                }
            },readResource("remoting-with-threads.xml"));

        CurrentConnectorAndController current = CurrentConnectorAndController.create(services, RemotingServices.SUBSYSTEM_ENDPOINT, RemotingServices.serverServiceName("test-connector"));

        //Test that write property reloads the connector
        ModelNode write = new ModelNode();
        write.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        write.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
        write.get(OP_ADDR).add(SUBSYSTEM, RemotingExtension.SUBSYSTEM_NAME).add(CommonAttributes.CONNECTOR, "test-connector").add(CommonAttributes.PROPERTY, "org.xnio.Options.WORKER_ACCEPT_THREADS");
        write.get(NAME).set(VALUE);
        write.get(VALUE).set(2);
        ModelNode result = services.executeOperation(write);
        assertFalse(result.get(FAILURE_DESCRIPTION).toString(), result.hasDefined(FAILURE_DESCRIPTION));
        assertEquals(2, services.readWholeModel().get(SUBSYSTEM, RemotingExtension.SUBSYSTEM_NAME, CommonAttributes.CONNECTOR, "test-connector", CommonAttributes.PROPERTY, "org.xnio.Options.WORKER_ACCEPT_THREADS").require(VALUE).asInt());
        current.updateCurrentEndpoint(true);
        current.updateCurrentConnector(false);

        //remove property
        ModelNode remove = write.clone();
        remove.get(OP).set(REMOVE);
        remove.remove(NAME);
        remove.remove(VALUE);
        result = services.executeOperation(remove);
        assertFalse(result.get(FAILURE_DESCRIPTION).toString(), result.hasDefined(FAILURE_DESCRIPTION));
        assertFalse(services.readWholeModel().get(SUBSYSTEM, RemotingExtension.SUBSYSTEM_NAME, CommonAttributes.CONNECTOR, "test-connector", CommonAttributes.PROPERTY, "org.xnio.Options.WORKER_ACCEPT_THREADS").isDefined());
        current.updateCurrentEndpoint(true);
        current.updateCurrentConnector(false);

        //TODO property
        ModelNode add = remove.clone();
        add.get(OP).set(ADD);
        add.get(VALUE).set(1);
        result = services.executeOperation(add);
        assertFalse(result.get(FAILURE_DESCRIPTION).toString(), result.hasDefined(FAILURE_DESCRIPTION));
        assertEquals(1, services.readWholeModel().get(SUBSYSTEM, RemotingExtension.SUBSYSTEM_NAME, CommonAttributes.CONNECTOR, "test-connector", CommonAttributes.PROPERTY, "org.xnio.Options.WORKER_ACCEPT_THREADS").require(VALUE).asInt());
        current.updateCurrentEndpoint(true);
        current.updateCurrentConnector(false);
    }

    @Test @Ignore("AS7-2717")
    public void testSubsystemWithBadConnectorProperty() throws Exception {
        final int port = 12345;
        KernelServices services = installInController(new AdditionalInitialization(){
                @Override
                protected void setupController(ControllerInitializer controllerInitializer) {
                    controllerInitializer.addSocketBinding("test", port);
                }

            },readResource("remoting-with-bad-connector-property.xml"));

        try {
            services.getContainer().getRequiredService(RemotingServices.SUBSYSTEM_ENDPOINT);
            fail("Expected no " + RemotingServices.SUBSYSTEM_ENDPOINT);
        } catch (ServiceNotFoundException expected) {
        }

        try {
            services.getContainer().getRequiredService(RemotingServices.serverServiceName("test-connector"));
            fail("Expected no " + RemotingServices.serverServiceName("test-connector"));
        } catch (ServiceNotFoundException expected) {
        }
    }

    /**
     * Tests that the outbound connections configured in the remoting subsytem are processed and services
     * are created for them
     *
     * @throws Exception
     */
    @Test
    public void testOutboundConnections() throws Exception {
        final int outboundSocketBindingPort = 6799;
        final int socketBindingPort = 1234;
        KernelServices services = installInController(new AdditionalInitialization(){
                @Override
                protected void setupController(ControllerInitializer controllerInitializer) {
                    controllerInitializer.addSocketBinding("test", socketBindingPort);
                    controllerInitializer.addRemoteOutboundSocketBinding("dummy-outbound-socket", "localhost", outboundSocketBindingPort);
                    controllerInitializer.addRemoteOutboundSocketBinding("other-outbound-socket", "localhost", outboundSocketBindingPort);
                }

            },readResource("remoting-with-outbound-connections.xml"));

        ServiceController<?> endPointService = services.getContainer().getRequiredService(RemotingServices.SUBSYSTEM_ENDPOINT);
        assertNotNull("Endpoint service was null", endPointService);

        final String remoteOutboundConnectionName = "remote-conn1";
        ServiceName remoteOutboundConnectionServiceName = RemoteOutboundConnectionService.REMOTE_OUTBOUND_CONNECTION_BASE_SERVICE_NAME.append(remoteOutboundConnectionName);
        ServiceController<?> remoteOutboundConnectionService = services.getContainer().getRequiredService(remoteOutboundConnectionServiceName);
        assertNotNull("Remote outbound connection service for outbound connection:" + remoteOutboundConnectionName + " was null", remoteOutboundConnectionService);


        final String localOutboundConnectionName = "local-conn1";
        ServiceName localOutboundConnectionServiceName = LocalOutboundConnectionService.LOCAL_OUTBOUND_CONNECTION_BASE_SERVICE_NAME.append(localOutboundConnectionName);
        ServiceController<?> localOutboundConnectionService = services.getContainer().getRequiredService(localOutboundConnectionServiceName);
        assertNotNull("Local outbound connection service for outbound connection:" + localOutboundConnectionName + " was null", localOutboundConnectionService);
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
    protected void validateXml(String original, String marshalled) throws Exception {
        // TODO: Can't and shouldn't rely on string equality check because if the original subsystem xml had a
        // namespace of 1.0 and the current is 1.1, then the marshalled subsystem xml will have the current == 1.1
        // value. So string equality won't work out here
        //assertEquals(original, marshalled);
        // let's just delegate it to the base class
        super.validateXml(original, marshalled);
    }

    private static class CurrentConnectorAndController {
        final KernelServices services;
        final ServiceName endpointName;
        final ServiceName connectorName;
        Object currentEndpoint;
        Object currentConnector;

        CurrentConnectorAndController(KernelServices services, ServiceName endpointName, ServiceName connectorName) {
            this.services = services;
            this.endpointName = endpointName;
            this.connectorName = connectorName;
            this.currentEndpoint = loadCurrentEndpoint(services);
            this.currentConnector = loadCurrentConnector(services);
        }

        static CurrentConnectorAndController create(KernelServices services, ServiceName endpointName, ServiceName connectorName) {
            return new CurrentConnectorAndController(services, endpointName, connectorName);
        }

        Object loadCurrentEndpoint(KernelServices services) {
            ServiceController<?> endPointService = services.getContainer().getRequiredService(endpointName);
            assertNotNull(endPointService);
            Object endpoint = endPointService.getValue();
            assertNotNull(endpoint);
            return endpoint;
        }

        Object loadCurrentConnector(KernelServices services) {
            ServiceController<?> connectorService = services.getContainer().getRequiredService(connectorName);
            assertNotNull(connectorService);
            Object connector = connectorService.getValue();
            assertNotNull(connector);
            return connector;
        }

        void updateCurrentEndpoint(final boolean equals) throws Exception {
            this.currentEndpoint = checkStatus(this.currentEndpoint, loadCurrentEndpoint(services), equals);
        }

        void updateCurrentConnector(final boolean equals) throws Exception {
            this.currentConnector = checkStatus(this.currentConnector, loadCurrentConnector(services), equals);
        }


        Object checkStatus(Object oldObject, Object newObject, boolean equals) {
            if (!equals) {
                assertNotSame(oldObject, newObject);
            } else {
                assertSame(oldObject, newObject);
            }
            return newObject;
        }
    }
}
