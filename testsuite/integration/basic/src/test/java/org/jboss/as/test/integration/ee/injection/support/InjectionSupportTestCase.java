/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ee.injection.support;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 *
 * @author Martin Kouba
 */
public abstract class InjectionSupportTestCase {

    protected static WebArchive createTestArchiveBase() {
        return ShrinkWrap.create(WebArchive.class)
                .addClasses(Alpha.class, Bravo.class, Charlie.class, ComponentInterceptorBinding.class, ComponentInterceptor.class, InjectionSupportTestCase.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    public static final Class<?>[] constructTestsHelperClasses = new Class<?>[] { AroundConstructInterceptor.class,
            AroundConstructBinding.class, StringProducer.class, ProducedString.class };

    @ArquillianResource
    protected URL contextPath;

    protected String doGetRequest(String path) throws IOException, ExecutionException, TimeoutException {
        return HttpRequest.get(contextPath + path, 10, TimeUnit.SECONDS);
    }

}
