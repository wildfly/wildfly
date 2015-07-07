package com.redhat.gss.redhat_support_lib.web;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;

import com.redhat.gss.redhat_support_lib.helpers.ConfigHelper;

public class CustomHttpEngine extends ApacheHttpClient4Engine {

    public CustomHttpEngine(ConfigHelper config) {
        super();
        HttpClientBuilder client = HttpClientBuilder.create();

        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        if (config.getProxyUrl() != null) {
            HttpHost proxy = new HttpHost(config.getProxyUrl().getHost(),
                    config.getProxyPort());
            client.setProxy(proxy);
        }
        if (config.getProxyUser() != null && config.getProxyPassword() != null) {
            credsProvider.setCredentials(new AuthScope(config.getProxyUrl()
                    .getHost(), config.getProxyPort()),
                    new UsernamePasswordCredentials(config.getProxyUser(),
                            config.getProxyPassword()));
            client.setDefaultCredentialsProvider(credsProvider);
        }

        try {
            client.setSslcontext(createGullibleSslContext());
        } catch (KeyManagementException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        this.httpClient = client.build();
    }

    public static TrustManager[] gullibleManagers = new TrustManager[] { new X509TrustManager() {

        public void checkClientTrusted(X509Certificate[] arg0, String arg1)
                throws CertificateException {
            // TODO Auto-generated method stub

        }

        public void checkServerTrusted(X509Certificate[] arg0, String arg1)
                throws CertificateException {
            // TODO Auto-generated method stub

        }

        public X509Certificate[] getAcceptedIssuers() {
            // TODO Auto-generated method stub
            return null;
        }
    } };

    public static SSLContext createGullibleSslContext()
            throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext ctx = SSLContext.getInstance("SSL");
        ctx.init(null, gullibleManagers, new SecureRandom());
        return ctx;
    }

}
