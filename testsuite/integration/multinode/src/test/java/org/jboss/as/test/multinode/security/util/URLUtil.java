/*
 * Copyright 2016 Red Hat, Inc.
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

package org.jboss.as.test.multinode.security.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;

import javax.xml.bind.JAXBException;

import org.jboss.as.test.multinode.security.api.EJBRequest;
import org.jboss.as.test.multinode.security.api.ServerConfigs.ServerConfig;
import org.jboss.as.test.multinode.security.api.TestConfig;
import org.jboss.logging.Logger;

/**
 * @author bmaxwell
 *
 */
public class URLUtil {

    private static Logger log = Logger.getLogger(URLUtil.class.getName());

    public static InputStream openConnectionWithBasicAuth(ServerConfig serverConfig, String path, String params, TestConfig.Credentials credentials) throws MalformedURLException, IOException {
        if(params == null) params = "";
        return openConnectionWithBasicAuth(String.format("http://%s:%d/%s?%s", serverConfig.getHost(), serverConfig.getHttpPort(), path, params), credentials.getUsername(), credentials.getPassword());
    }

    public static InputStream openConnectionWithBasicAuth(String host, Integer port, String path, String params, String username, String password) throws MalformedURLException, IOException {
        if(params == null) params = "";
        return openConnectionWithBasicAuth(String.format("%s:%d/%s?%s", host, port, path, params), username, password);
    }

    public static InputStream openConnectionWithBasicAuth(String host, Integer port, String path, String username, String password) throws MalformedURLException, IOException {
        return openConnectionWithBasicAuth(String.format("%s:%d/%s", host, port, path), username, password);
    }

    public static InputStream openConnectionWithBasicAuth(String host, Integer port, String username, String password) throws MalformedURLException, IOException {
        return openConnectionWithBasicAuth(String.format("%s:%d", host, port), username, password);
    }

    public static InputStream openConnectionWithBasicAuth(String urlPath, String username, String password)
            throws MalformedURLException, IOException {
        String authStringEnc = Base64.getEncoder().encodeToString(String.format("%s:%s", username, password).getBytes());
        URL url = new URL(urlPath);
        URLConnection urlConnection = url.openConnection();
        urlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc);
        log.debug("URL:" + urlPath);
        return urlConnection.getInputStream();
    }

    public static InputStream openConnectionWithBasicAuth(ServerConfig serverConfig, String urlPath, TestConfig.Credentials credentials, EJBRequest ejbRequest) throws MalformedURLException, IOException {
        try {
            String path = String.format("http://%s:%d/%s?%s", serverConfig.getHost(), serverConfig.getHttpPort(), urlPath, ejbRequest.getURLParams());
            String authStringEnc = Base64.getEncoder().encodeToString(String.format("%s:%s", credentials.getUsername(), credentials.getPassword()).getBytes());
            URL url = new URL(path);
            URLConnection urlConnection = url.openConnection();
            urlConnection.setRequestProperty("Authorization", "Basic " + authStringEnc);
            urlConnection.setRequestProperty("ejbRequest", ejbRequest.marshall());
            log.debug("URL:" + urlPath);
            return urlConnection.getInputStream();
        } catch(JAXBException je) {
            throw new IOException(je);
        }
    }

    public static void close(InputStream is) {
        if(is != null) {
            try {
                is.close();
            } catch(Exception e) {
            }
        }
    }

    public static String readInputStreamToString(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String inputLine;
        while ((inputLine = in.readLine()) != null)
            sb.append(inputLine);
        return sb.toString();
    }
}