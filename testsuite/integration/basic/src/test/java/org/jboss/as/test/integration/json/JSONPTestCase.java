/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.json;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.json.Json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import jakarta.json.JsonArray;
import jakarta.json.JsonReader;
import java.io.StringReader;
import java.net.URL;
import java.util.concurrent.TimeUnit;

/**
 * JSON-P 1.1 smoke test
 *
 * @author Rostislav Svoboda
 */
@ExtendWith(ArquillianExtension.class)
@RunAsClient
public class JSONPTestCase {

    @ArquillianResource
    private URL url;

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "jsonp11-test.war").addClasses(JSONPServlet.class);
    }

    @Test
    public void testJsonServlet() throws Exception {
        final String result = HttpRequest.get(url + "json", 5, TimeUnit.SECONDS);

        JsonReader jsonReader = Json.createReader(new StringReader(result));
        JsonArray array = jsonReader.readArray();
        jsonReader.close();

        assertEquals(2, array.size());
    }
}
