/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.messaging.jms.localtransaction;

import static org.jboss.shrinkwrap.api.ShrinkWrap.create;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the behaviour of allowing local transactions for a JMS session from a Servlet.
 *
 * Default behaviour is to disallow it.
 * It can be overridden by specifying allow-local-transactions=true on the pooled-connection-factory resource.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
public class LocalTransactionTestCase {

    @ArquillianResource
    private URL url;

    @Deployment
    public static WebArchive createArchive() {
        return create(WebArchive.class, "LocalTransactionTestCase.war")
                .addClass(MessagingServlet.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void testAllowLocalTransactions() throws Exception {
        callServlet(true);
    }

    @Test
    public void testDisallowLocalTransactions() throws Exception {
        callServlet(false);
    }

    private void callServlet(boolean allowLocalTransactions) throws Exception {
        URL url = new URL(this.url.toExternalForm() + "LocalTransactionTestCase?allowLocalTransactions=" + allowLocalTransactions);
        String reply  = HttpRequest.get(url.toExternalForm(), 10, TimeUnit.SECONDS);
        assertNotNull(reply);
        assertEquals(allowLocalTransactions, Boolean.valueOf(reply));
    }

}
