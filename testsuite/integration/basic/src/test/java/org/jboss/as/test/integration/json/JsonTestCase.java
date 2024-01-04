/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.json;

import java.io.StringReader;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import jakarta.json.Json;
import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParser.Event;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JsonTestCase {

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "jsonp-test.war")
                .addClasses(JsonTestCase.class, JsonServlet.class);
    }

    @ArquillianResource
    private URL url;

    @Test
    public void testJsonServlet() throws Exception {
        final String result = HttpRequest.get(url + "json", 10, TimeUnit.SECONDS);
        final JsonParser parser = Json.createParser(new StringReader(result));
        String key = null;
        String value = null;
        while (parser.hasNext()) {
            final Event event = parser.next();
            switch (event) {
                case KEY_NAME:
                    key = parser.getString();
                    break;
                case VALUE_STRING:
                    value = parser.getString();
                    break;
            }
        }
        parser.close();
        Assert.assertEquals("Key should be \"name\"", "name", key);
        Assert.assertEquals("Value should be \"value\"", "value", value);
    }
}
