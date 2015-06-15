package com.redhat.gss.redhat_support_lib.web;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPHTTPClient;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;

import com.redhat.gss.redhat_support_lib.errors.FTPException;
import com.redhat.gss.redhat_support_lib.filters.RedHatCookieFilter;
import com.redhat.gss.redhat_support_lib.filters.UserAgentFilter;
import com.redhat.gss.redhat_support_lib.helpers.ConfigHelper;

public class ConnectionManager {

    public static final int CONNECTION_POOL_SIZE = 10;

    ResteasyClientBuilder clientBuilder;
    ConfigHelper config = null;
    ResteasyClient client = null;

    public ConnectionManager(ConfigHelper config) {
        this.config = config;
        clientBuilder = new ResteasyClientBuilder()
                .connectionPoolSize(CONNECTION_POOL_SIZE);
        clientBuilder.connectionPoolSize(CONNECTION_POOL_SIZE);
        clientBuilder.connectionTTL(config.getTimeout(), TimeUnit.MILLISECONDS);
        clientBuilder.socketTimeout(config.getTimeout(), TimeUnit.MILLISECONDS);
        CustomHttpEngine httpEngine = new CustomHttpEngine(config);
        clientBuilder.httpEngine(httpEngine);
        if (config.isDevel()) {
            clientBuilder.disableTrustManager();
        }
        if (config.getProxyUrl() != null) {
            clientBuilder.defaultProxy(config.getProxyUrl().getHost(),
                    config.getProxyPort());
        }
    }

    public ConnectionManager(ConfigHelper config,
            ResteasyClientBuilder clientBuilder) {
        this.config = config;
        this.clientBuilder = clientBuilder;
    }

    public ResteasyClient getConnection() throws MalformedURLException {
        if (client == null) {
            // setting classloader to RESTEasy jaxrs classloader
            Thread t = Thread.currentThread();
            ClassLoader old = t.getContextClassLoader();
            t.setContextClassLoader(MultipartFormDataOutput.class
                    .getClassLoader());
            try {
                client = clientBuilder.build();
                setProviders();

            } finally {
                // setting classloader back to original
                t.setContextClassLoader(old);
            }
            if (config.getUsername() != null) {
                client.register(new BasicAuthentication(config.getUsername(),
                        config.getPassword()));
            }
            client.register(new UserAgentFilter(config.getUserAgent()));
            if (config.getCookies() != null) {
                client.register(new RedHatCookieFilter(config.getCookies()));
            }
        }
        return client;
    }

    public ConfigHelper getConfig() {
        return config;
    }

    public FTPClient getFTP() throws IOException, FTPException {
        FTPClient ftp = null;
        System.out.println("ftp username, ftp password: "
                + config.getFtpUsername() + ", " + config.getFtpPassword());
        if (config.getProxyUrl() == null) {
            ftp = new FTPClient();
            System.out.println("created new ftp: " + ftp);
        } else {
            ftp = new FTPHTTPClient(config.getProxyUrl().getHost(),
                    config.getProxyPort(), config.getProxyUser(),
                    config.getProxyPassword());
        }
        System.out.println("ftpHost, ftpPort: " + config.getFtpHost() + ", "
                + config.getFtpPort());
        ftp.connect(config.getFtpHost(), config.getFtpPort());
        if (!ftp.login(config.getFtpUsername(), config.getFtpPassword())) {
            throw new FTPException("Error during FTP login");
        }
        return ftp;
    }

    private void setProviders() {
            client.register(org.jboss.resteasy.plugins.providers.DataSourceProvider.class);
            client.register(org.jboss.resteasy.plugins.providers.DocumentProvider.class);
            client.register(org.jboss.resteasy.plugins.providers.DefaultTextPlain.class);
            client.register(org.jboss.resteasy.plugins.providers.StringTextStar.class);
            client.register(org.jboss.resteasy.plugins.providers.SourceProvider.class);
            client.register(org.jboss.resteasy.plugins.providers.InputStreamProvider.class);
            client.register(org.jboss.resteasy.plugins.providers.ReaderProvider.class);
            client.register(org.jboss.resteasy.plugins.providers.ByteArrayProvider.class);
            client.register(org.jboss.resteasy.plugins.providers.FormUrlEncodedProvider.class);
            client.register(org.jboss.resteasy.plugins.providers.JaxrsFormProvider.class);
            client.register(org.jboss.resteasy.plugins.providers.FileProvider.class);
            client.register(org.jboss.resteasy.plugins.providers.FileRangeWriter.class);
            client.register(org.jboss.resteasy.plugins.providers.StreamingOutputProvider.class);
            client.register(org.jboss.resteasy.plugins.providers.IIOImageProvider.class);
            client.register(org.jboss.resteasy.plugins.providers.SerializableProvider.class);
            client.register(org.jboss.resteasy.plugins.interceptors.CacheControlFeature.class);
            client.register(org.jboss.resteasy.plugins.interceptors.encoding.AcceptEncodingGZIPInterceptor.class);
            client.register(org.jboss.resteasy.plugins.interceptors.encoding.AcceptEncodingGZIPFilter.class);
            client.register(org.jboss.resteasy.plugins.interceptors.encoding.ClientContentEncodingAnnotationFeature.class);
            client.register(org.jboss.resteasy.plugins.interceptors.encoding.GZIPDecodingInterceptor.class);
            client.register(org.jboss.resteasy.plugins.interceptors.encoding.GZIPEncodingInterceptor.class);
            client.register(org.jboss.resteasy.plugins.interceptors.encoding.ServerContentEncodingAnnotationFeature.class);
            clientBuilder.register(org.jboss.resteasy.plugins.providers.DataSourceProvider.class);
            clientBuilder.register(org.jboss.resteasy.plugins.providers.DocumentProvider.class);
            clientBuilder.register(org.jboss.resteasy.plugins.providers.DefaultTextPlain.class);
            clientBuilder.register(org.jboss.resteasy.plugins.providers.StringTextStar.class);
            clientBuilder.register(org.jboss.resteasy.plugins.providers.SourceProvider.class);
            clientBuilder.register(org.jboss.resteasy.plugins.providers.InputStreamProvider.class);
            clientBuilder.register(org.jboss.resteasy.plugins.providers.ReaderProvider.class);
            clientBuilder.register(org.jboss.resteasy.plugins.providers.ByteArrayProvider.class);
            clientBuilder.register(org.jboss.resteasy.plugins.providers.FormUrlEncodedProvider.class);
            clientBuilder.register(org.jboss.resteasy.plugins.providers.JaxrsFormProvider.class);
            clientBuilder.register(org.jboss.resteasy.plugins.providers.FileProvider.class);
            clientBuilder.register(org.jboss.resteasy.plugins.providers.FileRangeWriter.class);
            clientBuilder.register(org.jboss.resteasy.plugins.providers.StreamingOutputProvider.class);
            clientBuilder.register(org.jboss.resteasy.plugins.providers.IIOImageProvider.class);
            clientBuilder.register(org.jboss.resteasy.plugins.providers.SerializableProvider.class);
            clientBuilder.register(org.jboss.resteasy.plugins.interceptors.CacheControlFeature.class);
            clientBuilder.register(org.jboss.resteasy.plugins.interceptors.encoding.AcceptEncodingGZIPInterceptor.class);
            clientBuilder.register(org.jboss.resteasy.plugins.interceptors.encoding.AcceptEncodingGZIPFilter.class);
            clientBuilder.register(org.jboss.resteasy.plugins.interceptors.encoding.ClientContentEncodingAnnotationFeature.class);
            clientBuilder.register(org.jboss.resteasy.plugins.interceptors.encoding.GZIPDecodingInterceptor.class);
            clientBuilder.register(org.jboss.resteasy.plugins.interceptors.encoding.GZIPEncodingInterceptor.class);
            clientBuilder.register(org.jboss.resteasy.plugins.interceptors.encoding.ServerContentEncodingAnnotationFeature.class);
    }
}