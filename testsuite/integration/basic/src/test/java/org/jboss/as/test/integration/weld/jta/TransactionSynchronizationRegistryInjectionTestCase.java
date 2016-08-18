package org.jboss.as.test.integration.weld.jta;

import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class TransactionSynchronizationRegistryInjectionTestCase {

    @Inject
    CdiBean cdiBean;

    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class)
                .addPackage(TransactionSynchronizationRegistryInjectionTestCase.class.getPackage())
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @Test
    public void testTransactionSynchronizationRegistryIsInjected() {
        Assert.assertTrue(cdiBean.isTransactionSynchronizationRegistryInjected());
    }
}
