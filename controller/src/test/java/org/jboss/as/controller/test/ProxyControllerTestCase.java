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

import java.util.concurrent.CancellationException;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.client.ExecutionContext;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */

public class ProxyControllerTestCase extends AbstractProxyControllerTest {

    @Override
    protected ProxyController createProxyController(final ModelController targetController, final PathAddress proxyNodeAddress) {
        return new TestProxyController(targetController, proxyNodeAddress);
    }

    static class TestProxyController implements ProxyController {
        private final ModelController targetController;
        private final PathAddress proxyNodeAddress;

        public TestProxyController(final ModelController targetController, final PathAddress proxyNodeAddress) {
            this.targetController = targetController;
            this.proxyNodeAddress = proxyNodeAddress;
        }

        @Override
        public PathAddress getProxyNodeAddress() {
            return proxyNodeAddress;
        }

        @Override
        public OperationResult execute(final ExecutionContext executionContext, final ResultHandler resultHandler) {
            return targetController.execute(executionContext, resultHandler);
        }

        @Override
        public ModelNode execute(final ExecutionContext executionContext) throws CancellationException {
            return targetController.execute(executionContext);
        }
    }
}
