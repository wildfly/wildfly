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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.as.controller.NewModelController;
import org.jboss.as.controller.NewModelController.OperationTransaction;
import org.jboss.as.controller.NewProxyController;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.dmr.ModelNode;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class ProxyControllerTestCase extends AbstractProxyControllerTest {

    static ExecutorService fakeProtocolExecutor;

    @BeforeClass
    public static void createExecutor() {
        fakeProtocolExecutor = Executors.newCachedThreadPool();
    }

    @AfterClass
    public static void stopExecutor() {
        fakeProtocolExecutor.shutdownNow();
    }

    @Override
    protected NewProxyController createProxyController(final NewModelController proxiedController, final PathAddress proxyNodeAddress) {
        return new TestProxyController(proxiedController, proxyNodeAddress);
    }

    class TestProxyController implements NewProxyController {
        private final NewModelController targetController;
        private final PathAddress proxyNodeAddress;

        public TestProxyController(final NewModelController targetController, final PathAddress proxyNodeAddress) {
            this.targetController = targetController;
            this.proxyNodeAddress = proxyNodeAddress;
        }

        @Override
        public PathAddress getProxyNodeAddress() {
            return proxyNodeAddress;
        }

        @Override
        public void execute(final ModelNode operation, final OperationMessageHandler handler, final ProxyOperationControl control, final OperationAttachments attachments) {
            final CountDownLatch latch = new CountDownLatch(1);

            fakeProtocolExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    ModelNode node = targetController.execute(operation, handler, new ProxyOperationControl() {

                        @Override
                        public void operationPrepared(final OperationTransaction transaction, final ModelNode result) {
                            final CountDownLatch completedTx = new CountDownLatch(1);
                            control.operationPrepared(new OperationTransaction() {

                                @Override
                                public void rollback() {
                                    fakeProtocolExecutor.execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            transaction.rollback();
                                            completedTx.countDown();
                                        }
                                    });
                                }

                                @Override
                                public void commit() {
                                    fakeProtocolExecutor.execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            transaction.commit();
                                            completedTx.countDown();
                                            latch.countDown();
                                        }
                                    });
                                }
                            }, result);
                            latch.countDown();
                            try {
                                completedTx.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void operationFailed(ModelNode response) {
                            control.operationFailed(response);
                            latch.countDown();
                        }

                        @Override
                        public void operationCompleted(ModelNode response) {
                            control.operationCompleted(response);
                            latch.countDown();
                        }
                    }, attachments);
                }
            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
