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
import java.util.concurrent.CancellationException;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.remote.RemoteProxyController;
import org.jboss.dmr.ModelNode;
import org.junit.After;
import org.junit.Before;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class EstablishConnectionRemoteProxyControllerTestCase extends AbstractProxyControllerTest {

    RemoteModelControllerSetup server;
    ModelController proxyController;
    PathAddress proxyNodeAddress;
    DelegatingProxyController testController = new DelegatingProxyController();

    @Before
    public void start() throws Exception {
        server = new RemoteModelControllerSetup(proxyController, 0);
        server.start();
        testController.setDelegate(RemoteProxyController.create(ModelControllerClient.Type.STANDALONE, InetAddress.getByName("localhost"), server.getPort(), proxyNodeAddress));
    }

    @After
    public void stop() {
        server.stop();
        testController.setDelegate(null);
    }

    @Override
    protected ProxyController createProxyController(final ModelController targetController, final PathAddress proxyNodeAddress) {
        this.proxyController = targetController;
        this.proxyNodeAddress = proxyNodeAddress;
        return testController;
    }

    private static class DelegatingProxyController implements ProxyController {

        ProxyController delegate;

        void setDelegate(ProxyController delegate) {
            this.delegate = delegate;
        }

        @Override
        public Cancellable execute(ModelNode operation, ResultHandler handler) {
            return delegate.execute(operation, handler);
        }

        @Override
        public ModelNode execute(ModelNode operation) throws CancellationException, OperationFailedException {
            return delegate.execute(operation);
        }

        @Override
        public PathAddress getProxyNodeAddress() {
            return delegate.getProxyNodeAddress();
        }

    }
}
