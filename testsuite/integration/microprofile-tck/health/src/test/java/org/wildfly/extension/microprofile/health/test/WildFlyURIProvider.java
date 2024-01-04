/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.health.test;

import java.lang.annotation.Annotation;
import java.net.URI;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.test.spi.enricher.resource.ResourceProvider;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 * @author Rostislav Svoboda (c) 2018 Red Hat inc.
 */
public class WildFlyURIProvider implements ResourceProvider {

    @Override
    public Object lookup(ArquillianResource arquillianResource, Annotation... annotations) {
        return URI.create("http://localhost:9990");
    }

    @Override
    public boolean canProvide(Class<?> type) {
        return type.isAssignableFrom(URI.class);
    }

}