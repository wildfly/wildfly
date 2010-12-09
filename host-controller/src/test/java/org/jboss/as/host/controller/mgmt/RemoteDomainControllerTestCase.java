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

package org.jboss.as.host.controller.mgmt;

import java.io.File;
import java.net.InetAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.jboss.as.domain.controller.DomainControllerImpl;
import org.jboss.as.domain.controller.mgmt.DomainControllerOperationHandler;
import org.jboss.as.host.controller.LocalFileRepository;
import org.jboss.as.host.controller.RemoteDomainControllerConnection;
import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.as.host.controller.StandardElementReaderRegistrar;
import org.jboss.as.model.DomainModel;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.staxmapper.XMLMapper;
import org.junit.Before;
import org.junit.Test;

/**
 * @author John Bailey
 */
public class RemoteDomainControllerTestCase {
    private ManagementCommunicationService communicationService;
    private DomainControllerOperationHandler operationHandler;
    private RemoteDomainControllerConnection domainControllerConnection;

    @Before
    public void setup() throws Exception {
        final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(10);

        communicationService = new ManagementCommunicationService();
        communicationService.getThreadFactoryInjector().inject(Executors.defaultThreadFactory());
        communicationService.getExecutorServiceInjector().inject(executorService);
        final NetworkInterfaceService networkInterfaceService = new NetworkInterfaceService("test", true, false, true, null);
        networkInterfaceService.start(null);
        communicationService.getInterfaceInjector().inject(networkInterfaceService.getValue());
        communicationService.getPortInjector().inject(12345);

        operationHandler = new DomainControllerOperationHandler();
        operationHandler.getThreadFactoryInjector().inject(Executors.defaultThreadFactory());
        operationHandler.getExecutorServiceInjector().inject(executorService);
        operationHandler.getLocalFileRepositoryInjector().inject(new LocalFileRepository(new HostControllerEnvironment(System.getProperties(), false, System.in, System.out, System.err, "test", InetAddress.getLocalHost(), 3223, InetAddress.getLocalHost(), 3223, "java")));

        final DomainControllerImpl domainController = new DomainControllerImpl();
        domainController.getDomainConfigDirInjector().inject(new File(getClass().getResource("/test/configuration").toURI()));
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));
        File deploymentDir = new File(tmpDir, "domain-deployments-" + ((int) Math.random()));
        deploymentDir.deleteOnExit();
        deploymentDir.mkdirs();
        domainController.getDomainDeploymentsDirInjector().inject(deploymentDir);
        final XMLMapper mapper = XMLMapper.Factory.create();
        StandardElementReaderRegistrar.Factory.getRegistrar().registerStandardDomainReaders(mapper);
        domainController.getXmlMapperInjector().inject(mapper);
        domainController.getScheduledExecutorServiceInjector().inject(executorService);
        domainController.start(null);

        operationHandler.getDomainControllerInjector().inject(domainController);
        operationHandler.start(null);
        communicationService.addHandler(operationHandler);

        communicationService.start(null);

        domainControllerConnection = new RemoteDomainControllerConnection("sm", InetAddress.getLocalHost(), 12345, InetAddress.getLocalHost(), 11223, null, 1000, Executors.newScheduledThreadPool(2), Executors.defaultThreadFactory());
    }

    @Test
    public void testRegister() throws Exception {
        final DomainModel domain = domainControllerConnection.register();
        System.out.println(domain);
    }
}
