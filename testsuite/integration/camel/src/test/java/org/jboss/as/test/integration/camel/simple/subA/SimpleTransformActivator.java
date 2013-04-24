/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.camel.simple.subA;

import java.util.Hashtable;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Test simple camel transform
 *
 * @author thomas.diesler@jboss.com
 * @since 21-Apr-2013
 */
public class SimpleTransformActivator implements BundleActivator {

    private DefaultCamelContext camelctx;

    @Override
    public void start(BundleContext context) throws Exception {
        camelctx = new DefaultCamelContext();
        camelctx.setName(context.getBundle().getSymbolicName());
        camelctx.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:start").transform(body().prepend("Hello "));
            }
        });
        camelctx.start();
        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put("name", camelctx.getName());
        context.registerService(CamelContext.class, camelctx, properties);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        camelctx.stop();
    }
}
