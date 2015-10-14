/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.http.util;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.CommonCookieAttributeHandler;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.CookieSpecRegistries;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicDomainHandler;
import org.apache.http.impl.cookie.BasicExpiresHandler;
import org.apache.http.impl.cookie.BasicMaxAgeHandler;
import org.apache.http.impl.cookie.BasicPathHandler;
import org.apache.http.impl.cookie.BasicSecureHandler;
import org.apache.http.impl.cookie.RFC6265CookieSpec;
import org.apache.http.protocol.HttpContext;

/**
 * Utility class with http/https utilities. Not to be confused with Apache {@link HttpClientUtils}.
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 * @author <a href="mailto:pskopek@redhat.com">Peter Skopek</a>
 * @author Stuart Douglas
 * @author Radoslav Husar
 * @version August 2015
 */
public class TestHttpClientUtils {

    /**
     * Returns https ready client.
     *
     * @param base
     * @return
     */
    public static HttpClient wrapHttpsClient(HttpClient base) {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            X509TrustManager tm = new X509TrustManager() {

                public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };
            ctx.init(null, new TrustManager[] { tm }, null);
            SSLSocketFactory ssf = new SSLSocketFactory(ctx, SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            ClientConnectionManager ccm = base.getConnectionManager();
            SchemeRegistry sr = ccm.getSchemeRegistry();
            sr.register(new Scheme("https", 443, ssf));
            return new DefaultHttpClient(ccm, base.getParams());
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a http client that sends cookies to every domain, not just the originating domain.
     * As we don't actually have a load balancer for the clustering tests, we use this instead.
     *
     * @return {@link CloseableHttpClient} that gives free cookies to everybody
     * @see TestHttpClientUtils#promiscuousCookieHttpClientBuilder()
     * @see <a href="http://tools.ietf.org/html/rfc6265">RFC6265 -  HTTP State Management Mechanism</a>
     */
    public static CloseableHttpClient promiscuousCookieHttpClient() {

        return promiscuousCookieHttpClientBuilder().build();
    }

    /**
     * Same as {@link TestHttpClientUtils#promiscuousCookieHttpClient()} but instead returns a builder that can be further configured.
     *
     * @return {@link HttpClientBuilder} of the http client that gives free cookies to everybody
     * @see TestHttpClientUtils#promiscuousCookieHttpClient()
     */
    public static HttpClientBuilder promiscuousCookieHttpClientBuilder() {

        HttpClientBuilder builder = HttpClients.custom();

        RegistryBuilder<CookieSpecProvider> registryBuilder = CookieSpecRegistries.createDefaultBuilder();
        Registry<CookieSpecProvider> promiscuousCookieSpecRegistry = registryBuilder.register("promiscuous", new PromiscuousCookieSpecProvider()).build();
        builder.setDefaultCookieSpecRegistry(promiscuousCookieSpecRegistry);

        RequestConfig requestConfig = RequestConfig.custom().setCookieSpec("promiscuous").build();
        builder.setDefaultRequestConfig(requestConfig);

        return builder;
    }


    private static class PromiscuousCookieSpecProvider implements CookieSpecProvider {

        @Override
        public CookieSpec create(HttpContext context) {
            return new PromiscuousCookieSpec();
        }
    }

    private static class PromiscuousCookieSpec extends RFC6265CookieSpec {

        PromiscuousCookieSpec(CommonCookieAttributeHandler... handlers) {
            super(
                    new BasicPathHandler(),
                    new BasicDomainHandler() {

                        @Override
                        public boolean match(Cookie cookie, CookieOrigin origin) {
                            return true;
                        }

                        @Override
                        public void validate(Cookie cookie, CookieOrigin origin) throws MalformedCookieException {
                            // Accept any
                        }
                    },
                    new BasicMaxAgeHandler(),
                    new BasicSecureHandler(),
                    new BasicExpiresHandler(new String[] {
                            DateUtils.PATTERN_RFC1123,
                            DateUtils.PATTERN_RFC1036,
                            DateUtils.PATTERN_ASCTIME
                    })
            );
        }
    }

}