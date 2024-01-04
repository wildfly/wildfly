/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.injection.support;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 *
 * @author Martin Kouba
 */
public abstract class InjectionSupportTestCase {

    protected static WebArchive createTestArchiveBase() {
        return ShrinkWrap.create(WebArchive.class)
                .addClasses(Alpha.class, Bravo.class, Charlie.class, ComponentInterceptorBinding.class, ComponentInterceptor.class, InjectionSupportTestCase.class)
                .addAsWebInfResource(new StringAsset("<beans bean-discovery-mode=\"all\"></beans>"), "beans.xml");
    }

    public static final Class<?>[] constructTestsHelperClasses = new Class<?>[] { AroundConstructInterceptor.class,
            AroundConstructBinding.class, StringProducer.class, ProducedString.class };

    @ArquillianResource
    protected URL contextPath;

    protected String doGetRequest(String path) throws IOException, ExecutionException, TimeoutException {
        return HttpRequest.get(contextPath + path, 10, TimeUnit.SECONDS);
    }

}
