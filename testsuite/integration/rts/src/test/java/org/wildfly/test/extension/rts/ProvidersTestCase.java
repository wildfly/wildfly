/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.extension.rts;

import jakarta.xml.bind.JAXBException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.jbossts.star.util.TxStatus;
import org.jboss.jbossts.star.util.TxSupport;
import org.jboss.jbossts.star.util.media.txstatusext.TransactionManagerElement;
import org.jboss.jbossts.star.util.media.txstatusext.TransactionStatisticsElement;
import org.jboss.shrinkwrap.api.spec.WebArchive;
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
public final class ProvidersTestCase extends AbstractTestCase {

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment
    public static WebArchive getDeployment() {
        return AbstractTestCase.getDeployment();
    }

    @Before
    public void before() {
        super.before();
        txSupport.startTx();
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

    protected String getBaseUrl() {
        return managementClient.getWebUri().toString();
    }

}
