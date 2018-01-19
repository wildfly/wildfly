/*
 * JBoss, Home of Professional Open Source
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.test.integration.ee8.temp.cdi20;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Simple test to verify that using EE8 switch, one can use CDI 2.0 feature. Here we use async events as that feature.
 *
 * @author <a href="mailto:manovotn@redhat.com">Matej Novotny</a>
 */
@RunWith(Arquillian.class)
public class CDI20SanityTest {

    @Deployment
    public static WebArchive deployment() {
        return ShrinkWrap.create(WebArchive.class, "sanity-cdi20-ee8-test-case.war")
            .addClasses(ObservingBean.class, CDI20SanityTest.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Inject
    BeanManager bm;

    @Test
    public void testCDI20FeatureCanBeUsed() throws Exception {
        // we assume EE 8 switch was set at this point
        Assert.assertNotNull(bm);
        CountDownLatch latch = new CountDownLatch(1);
        // use BM to fire async event, use latch to synchronize async operation
        bm.getEvent().fireAsync(latch);

        // assert that it was observed
        latch.await(5, TimeUnit.SECONDS);
        Assert.assertTrue(ObservingBean.EVENT_OBSERVED);
    }
}
