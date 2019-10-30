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
