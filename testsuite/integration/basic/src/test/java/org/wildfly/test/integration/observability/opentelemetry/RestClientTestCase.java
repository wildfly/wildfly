/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.integration.observability.opentelemetry;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.common.Assert;

@RunWith(Arquillian.class)
public class RestClientTestCase {

    @Deployment
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class);
        war.addClass(RestClientTestCase.class);
        war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @Test
    public void hasDefaultInjectedOpenTelemetry() {
        Client client = ClientBuilder.newClient();
        Assert.assertTrue(client.getConfiguration().getClasses().stream()
                .map(c -> c.getCanonicalName())
                .anyMatch(n ->"org.wildfly.extension.opentelemetry.api.OpenTelemetryClientRequestFilter".equals(n)));
    }
}
