package com.redhat.gss.redhat_support_lib.web;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPHTTPClient;
import org.jboss.resteasy.client.jaxrs.BasicAuthentication;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
//import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;

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
            // setting classloader to this
            Thread t = Thread.currentThread();
            ClassLoader old = t.getContextClassLoader();
            t.setContextClassLoader(this.getClass().getClassLoader());
            try {
                client = clientBuilder.build();
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
        if (config.getProxyUrl() == null) {
            ftp = new FTPClient();
        } else {
            ftp = new FTPHTTPClient(config.getProxyUrl().getHost(),
                    config.getProxyPort(), config.getProxyUser(),
                    config.getProxyPassword());
        }
        ftp.connect(config.getFtpHost(), config.getFtpPort());
        if (!ftp.login(config.getFtpUsername(), config.getFtpPassword())) {
            throw new FTPException("Error during FTP login");
        }
        return ftp;
    }
}
