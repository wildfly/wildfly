/*
* JBoss, Home of Professional Open Source.
* Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.controller.test;

import java.net.InetAddress;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.remote.RemoteProxyController;
import org.junit.After;
import org.junit.Before;

/**
 * Tests a proxy controller where the main side establishes a connection to the proxied side.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class EstablishConnectionRemoteProxyControllerTestCase extends AbstractProxyControllerTest {

    RemoteModelControllerSetup server;
    ModelController proxiedController;
    PathAddress proxyNodeAddress;
    DelegatingProxyController proxyController = new DelegatingProxyController();

    @Before
    public void start() throws Exception {

        setupNodes();

        server = new RemoteModelControllerSetup(proxiedController, 0);
        server.start();
        proxyController.setDelegate(RemoteProxyController.create(InetAddress.getByName("localhost"), server.getPort(), proxyNodeAddress));
    }

    @After
    public void stop() {
        server.stop();
        proxyController.setDelegate(null);
    }

    @Override
    protected ProxyController createProxyController(final ModelController proxiedController, final PathAddress proxyNodeAddress) {
        this.proxiedController = proxiedController;
        this.proxyNodeAddress = proxyNodeAddress;
        return proxyController;
    }
}
