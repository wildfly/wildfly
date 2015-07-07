package com.redhat.gss.redhat_support_lib.helpers;

import javax.ws.rs.core.Cookie;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

public class ConfigHelper {
    String username = null;
    char[] password = null;
    String url = "https://api.access.redhat.com";
    int proxyPort = -1;
    URL proxyUrl = null;
    String proxyUser = null;
    String proxyPassword = null;
    String ftpHost = "dropbox.redhat.com";
    int ftpPort = 21;
    String ftpUsername = null;
    String ftpPassword = null;
    boolean devel = false;
    int timeout = 500000;
    int maxConnections = 50;
    long ftpFileSize = 2000000L;
    String ftpDir = "/incoming";
    String userAgent = "redhat-support-lib-java";
    Map<String, Cookie> cookies = null;

    public ConfigHelper(String username, String password, String url,
            String proxyUser, String proxyPassword, URL proxyUrl,
            int proxyPort, String userAgent, boolean devel) {
        this.username = username;
        this.password = password.toCharArray();
        if (url != null) {
            this.url = url;

        }
        this.proxyUser = proxyUser;
        this.proxyPassword = proxyPassword;
        this.proxyUrl = proxyUrl;
        this.proxyPort = proxyPort;
        this.devel = devel;
        this.userAgent = userAgent;
    }

    public ConfigHelper(String username, String password, String url,
            String proxyUser, String proxyPassword, URL proxyUrl,
            int proxyPort, String userAgent, int timeout, boolean devel) {
        this.username = username;
        this.password = password.toCharArray();
        if (url != null) {
            this.url = url;

        }
        this.proxyUser = proxyUser;
        this.proxyPassword = proxyPassword;
        this.proxyUrl = proxyUrl;
        this.proxyPort = proxyPort;
        this.devel = devel;
        this.userAgent = userAgent;
        this.timeout = timeout;
    }

    public ConfigHelper(String url, String proxyUser, String proxyPassword,
            URL proxyUrl, int proxyPort, String userAgent,
            Map<String, Cookie> cookies, boolean devel) {
        if (url != null) {
            this.url = url;

        }
        this.proxyUser = proxyUser;
        this.proxyPassword = proxyPassword;
        this.proxyUrl = proxyUrl;
        this.proxyPort = proxyPort;
        this.devel = devel;
        this.userAgent = userAgent;
        this.cookies = cookies;
    }

    public ConfigHelper(String username, String password, String url,
            String proxyUser, String proxyPassword, URL proxyUrl,
            int proxyPort, String ftpHost, int ftpPort, String ftpUsername,
            String ftpPassword, boolean devel) {
        this.username = username;
        this.password = password.toCharArray();
        if (url != null) {
            this.url = url;

        }
        this.proxyUser = proxyUser;
        this.proxyPassword = proxyPassword;
        this.proxyUrl = proxyUrl;
        this.proxyPort = proxyPort;
        this.ftpHost = ftpHost;
        this.ftpPort = ftpPort;
        this.ftpUsername = ftpUsername;
        this.ftpPassword = ftpPassword;
        this.devel = devel;
    }

    public ConfigHelper(String configFileName) throws IOException {
        Properties props = ParseHelper.parseConfigFile(configFileName);
        this.username = props.getProperty("username");
        this.password = props.getProperty("password").toCharArray();
        if (props.getProperty("url") != null) {
            this.url = props.getProperty("url");

        }
        this.proxyUser = props.getProperty("proxyUser");
        this.proxyPassword = props.getProperty("proxyPassword");
        if (props.getProperty("proxyUrl") != null) {
            this.proxyUrl = new URL(props.getProperty("proxyUrl"));
        }
        if (props.getProperty("proxyPort") != null) {
            this.proxyPort = Integer.valueOf(props.getProperty("proxyPort"));
        }
        if (props.getProperty("ftpHost") != null) {
            this.ftpHost = props.getProperty("ftpHost");
        }
        if (props.getProperty("ftpPort") != null) {
            this.ftpPort = Integer.valueOf(props.getProperty("ftpPort"));
        }
        if (props.getProperty("ftpUsername") != null) {
            this.ftpUsername = props.getProperty("ftpUsername");
        }
        if (props.getProperty("ftpPassword") != null) {
            this.ftpPassword = props.getProperty("ftpPassword");
        }
        if (props.getProperty("devel") != null) {
            this.devel = Boolean.parseBoolean(props.getProperty("devel"));
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return new String(password);
    }

    public void setPassword(String password) {
        this.password = password.toCharArray();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public URL getProxyUrl() {
        return proxyUrl;
    }

    public void setProxyUrl(URL proxyUrl) {
        this.proxyUrl = proxyUrl;
    }

    public String getProxyUser() {
        return proxyUser;
    }

    public void setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    public String getFtpHost() {
        return ftpHost;
    }

    public void setFtpHost(String ftpHost) {
        this.ftpHost = ftpHost;
    }

    public int getFtpPort() {
        return ftpPort;
    }

    public void setFtpPort(int ftpPort) {
        this.ftpPort = ftpPort;
    }

    public String getFtpUsername() {
        return ftpUsername;
    }

    public void setFtpUsername(String ftpUsername) {
        this.ftpUsername = ftpUsername;
    }

    public String getFtpPassword() {
        return ftpPassword;
    }

    public void setFtpPassword(String ftpPassword) {
        this.ftpPassword = ftpPassword;
    }

    public boolean isDevel() {
        return devel;
    }

    public void setDevel(boolean devel) {
        this.devel = devel;
    }

    public long getFtpFileSize() {
        return ftpFileSize;
    }

    public void setFtpFileSize(long ftpFileSize) {
        this.ftpFileSize = ftpFileSize;
    }

    public String getFtpDir() {
        return ftpDir;
    }

    public void setFtpDir(String ftpDir) {
        this.ftpDir = ftpDir;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Map<String, Cookie> getCookies() {
        return cookies;
    }

    public void setCookies(Map<String, Cookie> cookies) {
        this.cookies = cookies;
    }
}
