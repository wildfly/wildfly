/*
* JBoss, Home of Professional Open Source.
* Copyright 2013, Red Hat Middleware LLC, and individual contributors
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
package org.wildfly.test.extension.rts;

import java.net.URL;

import javax.xml.bind.JAXBException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.jbossts.star.util.TxStatus;
import org.jboss.jbossts.star.util.TxSupport;
import org.jboss.jbossts.star.util.media.txstatusext.TransactionManagerElement;
import org.jboss.jbossts.star.util.media.txstatusext.TransactionStatisticsElement;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test if all providers work as required.
 *
 * Some of the media types and XML elements are covered in another tests. Therefore, they were emitted here.
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 *
 */
@RunAsClient
@RunWith(Arquillian.class)
public final class ProvidersTestCase {

    private static final String DEPLOYMENT_NAME = "test-deployment";

    private TxSupport txSupport;

    @ArquillianResource
    private URL deploymentUrl;

    /**
     * Just an empty deployment to make deploymentUrl injection possible.
     *
     * @return
     */
    @Deployment
    public static WebArchive getDeployment() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME + ".war");
    }

    @Before
    public void before() {
        final String transactionManagerUrl = getBaseUrl() + "rest-at-coordinator/tx/transaction-manager";

        txSupport = new TxSupport(transactionManagerUrl);
        txSupport.startTx();
    }

    @After
    public void after() {
        txSupport.rollbackTx();
    }

    @Test
    public void testTxStatusMediaType() {
        Assert.assertEquals(TxStatus.TransactionActive.name(), TxSupport.getStatus(txSupport.txStatus()));
    }

    @Test
    public void testTransactionManagerElement() throws JAXBException {
        TransactionManagerElement transactionManagerElement = txSupport.getTransactionManagerInfo();

        Assert.assertNotNull(transactionManagerElement);
        Assert.assertEquals(1, transactionManagerElement.getCoordinatorURIs().size());
    }

    @Test
    public void testTransactionStatisticsElement() throws JAXBException {
        TransactionStatisticsElement transactionStatisticsElement = txSupport.getTransactionStatistics();

        Assert.assertNotNull(transactionStatisticsElement);
        Assert.assertEquals(1, transactionStatisticsElement.getActive());
    }

    private String getBaseUrl() {
        if (deploymentUrl == null) {
            return null;
        }

        final int cutUntil = deploymentUrl.toString().indexOf(DEPLOYMENT_NAME);

        return deploymentUrl.toString().substring(0, cutUntil);
    }

}