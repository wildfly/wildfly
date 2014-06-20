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

package org.jboss.as.test.integration.management.interfaces;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.jboss.dmr.ModelNode;

/**
 * @author Ladislav Thon <lthon@redhat.com>
 */
public class HttpManagementInterface implements ManagementInterface {
    public static final String MANAGEMENT_REALM = "ManagementRealm";

    private final URI uri;
    private final HttpClient httpClient;

    public HttpManagementInterface(String uriScheme, String host, int port, String username, String password) {
        try {
            this.uri = new URI(uriScheme + "://" + host + ":" + port + "/management");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        this.httpClient = createHttpClient(host, port, username, password);
    }

    @Override
    public ModelNode execute(ModelNode operation) {
        String operationJson = operation.toJSONString(true);
        try {
            HttpPost post = new HttpPost(uri);
            post.setEntity(new StringEntity(operationJson, ContentType.APPLICATION_JSON));
            post.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
            HttpResponse response = httpClient.execute(post);
            return parseResponse(response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ModelNode parseResponse(HttpResponse response) {
        try {
            String content = EntityUtils.toString(response.getEntity());
            int status = response.getStatusLine().getStatusCode();
            ModelNode modelResponse;
            if (status == HttpStatus.SC_OK) {
                modelResponse = ModelNode.fromJSONString(content);
            } else {
                modelResponse = new ModelNode();
                modelResponse.get(OUTCOME).set(FAILED);
                modelResponse.get(FAILURE_DESCRIPTION).set(content);
            }
            return modelResponse;
        } catch (IOException e) {
            throw new RuntimeException("Unable to read response content as String");
        }
    }

    @Override
    public void close() {
        httpClient.getConnectionManager().shutdown();
    }

    private static HttpClient createHttpClient(String host, int port, String username, String password) {
        PoolingClientConnectionManager connectionPool = new PoolingClientConnectionManager();
        DefaultHttpClient httpClient = new DefaultHttpClient(connectionPool);
        SchemeRegistry schemeRegistry = httpClient.getConnectionManager().getSchemeRegistry();
        schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        try {
            schemeRegistry.register(new Scheme("https", 443, new SSLSocketFactory(new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }, new AllowAllHostnameVerifier())));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        httpClient.setHttpRequestRetryHandler(new StandardHttpRequestRetryHandler(5, true));
        httpClient.getCredentialsProvider().setCredentials(
                new AuthScope(host, port, MANAGEMENT_REALM, AuthPolicy.DIGEST),
                new UsernamePasswordCredentials(username, password)
        );

        return httpClient;
    }

    public static ManagementInterface create(String host, int port, String username, String password) {
        return new HttpManagementInterface("http", host, port, username, password);
    }

    public static ManagementInterface createSecure(String host, int port, String username, String password) {
        return new HttpManagementInterface("https", host, port, username, password);
    }
}
