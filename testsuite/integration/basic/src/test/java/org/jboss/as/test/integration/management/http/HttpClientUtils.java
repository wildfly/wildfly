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

package org.jboss.as.test.integration.management.http;

import static org.apache.http.util.EntityUtils.consumeQuietly;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.jboss.as.test.http.Authentication;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2013 Red Hat inc.
 */
public class HttpClientUtils {
    static DefaultHttpClient create(String host, int port) {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials(Authentication.USERNAME, Authentication.PASSWORD);
        httpClient.getCredentialsProvider().setCredentials(new AuthScope(host, port, "ManagementRealm"), creds);
        return httpClient;
    }

    static HttpResponse delete(DefaultHttpClient client, URL url) throws Exception {
        return send(client, new HttpDelete(url.toURI().toString()), false);
    }

    static HttpResponse get(DefaultHttpClient client, URL url, boolean consumeEntity) throws Exception {
        return send(client, new HttpGet(url.toURI().toString()), consumeEntity);
    }

    static HttpResponse post(DefaultHttpClient client, URL url, boolean consumeEntity) throws Exception {
        return post(client, url, null, consumeEntity);
    }

    static HttpResponse post(DefaultHttpClient client, URL url, ModelNode body, boolean consumeEntity) throws Exception {
        HttpPost post = new HttpPost(url.toURI());
        if (body != null) {
            StringEntity entity = new StringEntity(body.toJSONString(true));
            entity.setContentType("application/json");
            post.setEntity(entity);
        }
        return send(client, post, consumeEntity);
    }

    private static HttpResponse send(DefaultHttpClient client, HttpRequestBase request, boolean consumeEntity) throws IOException {
        HttpContext context = new BasicHttpContext();
        HttpResponse response = client.execute(request, context);
        assertNotNull(response);
        if (consumeEntity) {
            consumeQuietly(response.getEntity());
        }
        return response;
    }
}
