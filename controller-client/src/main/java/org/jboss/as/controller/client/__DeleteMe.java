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
package org.jboss.as.controller.client;

import java.net.ConnectException;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.as.protocol.mgmt.ManagementChannel;
import org.jboss.dmr.ModelNode;

/**
 * If I accidentally commit this file, feel free to delete it. It is for trying out the new protocol stuff
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class __DeleteMe {

    public static void main(String[] args) throws Exception {
        ExecutorService executorService = Executors.newCachedThreadPool();
        ProtocolChannelClient.Configuration<ManagementChannel> configuration = new ProtocolChannelClient.Configuration<ManagementChannel>();
        configuration.setEndpointName("endpoint");
        configuration.setUriScheme("remote");
        configuration.setUri(new URI("remote://localhost:9999"));
        configuration.setExecutor(executorService);
        ProtocolChannelClient<ManagementChannel> client = ProtocolChannelClient.create(configuration);

        while (true) {
            try {
                System.out.println("Attempting connection...");
                client.connect();
                break;
            } catch (ConnectException e) {
            }
        }
        ManagementChannel channel = client.openChannel("management");
        ModelControllerClient modelControllerClient = ModelControllerClient.Factory.create(channel);

        //ModelControllerClient modelControllerClient = ModelControllerClient.Factory.create("localhost", 9999);

        ModelNode op = new ModelNode();
        op.get("operation").set("read-resource");
        ModelNode addr = op.get("address");
        addr.add("management-interfaces", "native-interface");

        System.out.println(op);
        System.out.println(modelControllerClient.execute(op));

        final CountDownLatch latch = new CountDownLatch(1);
        ResultHandler handler = new ResultHandler() {

            @Override
            public void handleResultFragment(String[] location, ModelNode result) {
                System.out.println("Result" + result);
            }

            @Override
            public void handleResultComplete() {
                System.out.println("Complete");
                latch.countDown();
            }

            @Override
            public void handleFailed(ModelNode failureDescription) {
                System.out.println("Failed");
                latch.countDown();
            }

            @Override
            public void handleCancellation() {
                System.out.println("Cancelled");
                latch.countDown();
            }
        };
        OperationResult operationResult = modelControllerClient.execute(op, handler);
        latch.await();

//        IoUtils.safeClose(client);
//        executorService.shutdown();
//        executorService.awaitTermination(10, TimeUnit.SECONDS);
//        executorService.shutdownNow();

        //Connecting before started fails

        //Passing in something weird crashes the client
        //modelControllerClient.execute(new ModelNode().get("name").set("add"));

        System.out.println("--- Done");
    }

}
