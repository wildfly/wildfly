/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.transaction;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.integration.transactions.TestXAResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests to verify that transaction timeout setting is cleaned up after a http request
 * is done, and is not carried over to the next request processing.
 * See <a href="https://issues.redhat.com/browse/WFLY-16514">WFLY-16514</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class TransactionTimeoutLeakTestCase {
    @ArquillianResource
    private URL url;

    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "transaction-timeout-leak.war");
        war.addPackage(TestXAResource.class.getPackage());
        war.addClass(TransactionTimeoutLeakTestCase.class);
        war.addClass(TransactionTimeoutLeakServlet.class);
        war.addClass(HttpRequest.class);
        return war;
    }

    /**
     * This test first sends to TransactionTimeoutLeakServlet a batch of concurrent
     * requests having a short tx timeout value. This instructs the target servlet
     * to set a custom transaction timeout value. The test then verifies
     * the effective transaction timeout.
     * Next, the test sends a batch of concurrent requests having no tx timeout value.
     * This instructs the target servlet to use the server default transaction
     * timeout value. The test verifies that the default transaction timeout
     * value is used, and that the value used in the first batch should not be
     * leaked to the 2nd batch of requests.
     *
     * @throws Exception upon error
     */
    @Test
    public void testUserTransactionTimeoutLeak() throws Exception {
        final int threadCount = 10;
        final ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        test(2, threadCount, executorService);
        test(0, threadCount, executorService);
        executorService.shutdownNow();
    }

    private void test(int timeout, int threadCount, ExecutorService executorService) throws Exception {
        final String customTimeout = Integer.toString(timeout);
        final String[] expected = new String[threadCount];
        Arrays.fill(expected, timeout > 0 ? customTimeout : String.valueOf(300));

        final String queryString = timeout > 0 ? "timeout?second=" + customTimeout : "timeout";
        final String requestUrl = url.toExternalForm() + queryString;
        final List<Future<String>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executorService.submit(() -> {
                        String result;
                        try {
                            result = HttpRequest.get(requestUrl, 10, SECONDS);
                        } catch (Exception e) {
                            result = e.toString();
                        }
                        return result;
                    }
            ));
        }
        final String[] results = new String[threadCount];
        for (int i = 0; i < threadCount; i++) {
            results[i] = futures.get(i).get(2, TimeUnit.MINUTES);
        }
        Assert.assertArrayEquals(expected, results);
    }
}
