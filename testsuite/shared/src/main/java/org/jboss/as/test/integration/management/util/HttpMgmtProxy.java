/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.management.util;

import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.jboss.as.test.http.Authentication;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
public class HttpMgmtProxy {

    private static final String APPLICATION_JSON = "application/json";
    private URL url;
    private HttpClient httpClient;
    private HttpContext httpContext = new BasicHttpContext();

    public HttpMgmtProxy(URL mgmtURL) {
        this.url = mgmtURL;
        DefaultHttpClient httpClient = new DefaultHttpClient();
        UsernamePasswordCredentials creds = new UsernamePasswordCredentials(Authentication.USERNAME, Authentication.PASSWORD);
        httpClient.getCredentialsProvider().setCredentials(new AuthScope(url.getHost(), url.getPort(), "ManagementRealm"), creds);
        this.httpClient = httpClient;
    }

    public ModelNode sendGetCommand(String cmd) throws Exception {

        HttpGet get = new HttpGet(url.toURI().toString() + cmd);

        HttpResponse response = httpClient.execute(get, httpContext);
        String str = EntityUtils.toString(response.getEntity());

        return ModelNode.fromJSONString(str);
    }

    public ModelNode sendPostCommand(String address, String operation) throws Exception {
        return sendPostCommand(getOpNode(address, operation));
    }

    public ModelNode sendPostCommand(ModelNode cmd) throws Exception {

        String cmdStr = cmd.toJSONString(true);
        HttpPost post = new HttpPost(url.toURI());
        StringEntity entity = new StringEntity(cmdStr);
        entity.setContentType(APPLICATION_JSON);
        post.setEntity(entity);

        HttpResponse response = httpClient.execute(post, httpContext);
        String str = EntityUtils.toString(response.getEntity());

        return ModelNode.fromJSONString(str);
    }

    public static ModelNode getOpNode(String address, String operation) {
        ModelNode op = new ModelNode();

        // set address
        String[] pathSegments = address.split("/");
        ModelNode list = op.get("address").setEmptyList();
        for (String segment : pathSegments) {
            String[] elements = segment.split("=");
            list.add(elements[0], elements[1]);
        }
        op.get("operation").set(operation);
        return op;
    }
}
