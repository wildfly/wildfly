package com.redhat.gss.redhat_support_lib.web;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPHTTPClient;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;

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
        // setting classloader to RESTEasy jaxrs classloader
        Thread t = Thread.currentThread();
        ClassLoader old = t.getContextClassLoader();
        t.setContextClassLoader(ResteasyClientBuilder.class.getClassLoader());
        try {
            clientBuilder = new ResteasyClientBuilder().connectionPoolSize(CONNECTION_POOL_SIZE);
        } finally {
            // setting classloader back to original
            t.setContextClassLoader(old);
        }
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
            client = clientBuilder.build();
            if (config.getUsername() != null) {
                client.register(new BasicAuthentication(config.getUsername(),
                        config.getPassword()));
            }
            client.register(new UserAgentFilter(config.getUserAgent()));
            if (config.getCookies() != null) {
                client.register(new RedHatCookieFilter(config.getCookies()));
            }
            setWriters();
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

    private void setWriters() {
        try {
            // results in a ClassNotFoundException
            client.register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.MultipartReader"));
            client.register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.ListMultipartReader"));
            client.register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataReader"));
            client.register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.MultipartRelatedReader"));
            client.register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.MapMultipartFormDataReader"));
            client.register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.MultipartWriter"));
            client.register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataWriter"));
            client.register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.MultipartRelatedWriter"));
            client.register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.ListMultipartWriter"));
            client.register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.MapMultipartFormDataWriter"));
            client.register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.MultipartFormAnnotationReader"));
            client.register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.MultipartFormAnnotationWriter"));
            client.register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.MimeMultipartProvider"));
            client.register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.XopWithMultipartRelatedReader"));
            client.register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.XopWithMultipartRelatedWriter"));
            clientBuilder
                    .register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.MultipartReader"));
            clientBuilder
                    .register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.ListMultipartReader"));
            clientBuilder
                    .register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataReader"));
            clientBuilder
                    .register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.MultipartRelatedReader"));
            clientBuilder
                    .register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.MapMultipartFormDataReader"));
            clientBuilder
                    .register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.MultipartWriter"));
            clientBuilder
                    .register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataWriter"));
            clientBuilder
                    .register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.MultipartRelatedWriter"));
            clientBuilder
                    .register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.ListMultipartWriter"));
            clientBuilder
                    .register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.MapMultipartFormDataWriter"));
            clientBuilder
                    .register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.MultipartFormAnnotationReader"));
            clientBuilder
                    .register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.MultipartFormAnnotationWriter"));
            clientBuilder
                    .register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.MimeMultipartProvider"));
            clientBuilder
                    .register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.XopWithMultipartRelatedReader"));
            clientBuilder
                    .register(Thread.currentThread().getContextClassLoader().loadClass("org.jboss.resteasy.plugins.providers.multipart.XopWithMultipartRelatedWriter"));
        }
        catch(ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}