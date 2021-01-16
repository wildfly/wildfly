/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.test.integration.microprofile.opentracing;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

import java.net.SocketPermission;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import io.opentracing.Tracer;
import io.opentracing.contrib.tracerresolver.TracerFactory;
import io.opentracing.mock.MockTracer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.shared.TestSuiteEnvironment;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.opentracing.application.MockTracerFactory;
import org.wildfly.test.integration.microprofile.opentracing.application.TracerIdentityApplication;

/**
 * Testing correct tracer behavior when path contains regular expressions.
 * See https://github.com/opentracing-contrib/java-jaxrs/issues/114 for more details.
 * @author Sultan Zhantemirov (c) 2019 Red Hat, Inc.
 */
@RunWith(Arquillian.class)
public class WildCardTestCase {

    @Inject
    Tracer tracer;

    @ArquillianResource
    private URL url;

    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "WildcardPath.war")
                .addClass(TracerIdentityApplication.class)
                .addClass(HttpRequest.class)
                .addPackage(MockTracer.class.getPackage())
                .addClass(MockTracerFactory.class)
                .addAsServiceProvider(TracerFactory.class, MockTracerFactory.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsManifestResource(createPermissionsXmlAsset(
                    // Required for the HttpRequest.get()
                    new RuntimePermission("modifyThread"),
                    // Required for the HttpRequest.get()
                    new SocketPermission(TestSuiteEnvironment.getHttpAddress() + ":" + TestSuiteEnvironment.getHttpPort(), "connect,resolve")
                ), "permissions.xml");
    }

    @Test
    public void wildCardPath() throws Exception {
        String result = HttpRequest.get(url + "service-endpoint/test/1/hello", 10, TimeUnit.SECONDS);
        Assert.assertEquals("Path with regular expressions was not processed correctly","Hello from twoWildcard: 1, hello", result);
        Assert.assertEquals("Tracer should have finished spans",1, ((MockTracer) tracer).finishedSpans().size());
    }

}
