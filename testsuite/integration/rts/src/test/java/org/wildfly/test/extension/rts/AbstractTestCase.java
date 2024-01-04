/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.test.extension.rts;

import org.jboss.jbossts.star.util.TxSupport;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

/**
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public abstract class AbstractTestCase {

    protected static final String DEPLOYMENT_NAME = "test-deployment";

    protected TxSupport txSupport;

    public static WebArchive getDeployment() {
        return ShrinkWrap.create(WebArchive.class, DEPLOYMENT_NAME + ".war");
    }

    @Before
    public void before() {
        TxSupport.setTxnMgrUrl(getBaseUrl() + "/rest-at-coordinator/tx/transaction-manager");
        txSupport = new TxSupport();
    }

    @After
    public void after() {
        try {
            txSupport.rollbackTx();
        } catch (Throwable t) {
        }

        Assert.assertEquals(0, txSupport.txCount());
    }

    protected String getDeploymentUrl() {
        return getBaseUrl() + "/" + DEPLOYMENT_NAME + "/";
    }

    protected abstract String getBaseUrl();

}
