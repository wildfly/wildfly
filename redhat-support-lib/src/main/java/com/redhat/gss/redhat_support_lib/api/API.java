package com.redhat.gss.redhat_support_lib.api;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import com.redhat.gss.redhat_support_lib.helpers.ConfigHelper;
import com.redhat.gss.redhat_support_lib.infrastructure.Articles;
import com.redhat.gss.redhat_support_lib.infrastructure.Attachments;
import com.redhat.gss.redhat_support_lib.infrastructure.Cases;
import com.redhat.gss.redhat_support_lib.infrastructure.Comments;
import com.redhat.gss.redhat_support_lib.infrastructure.Entitlements;
import com.redhat.gss.redhat_support_lib.infrastructure.Ping;
import com.redhat.gss.redhat_support_lib.infrastructure.Problems;
import com.redhat.gss.redhat_support_lib.infrastructure.Products;
import com.redhat.gss.redhat_support_lib.infrastructure.Search;
import com.redhat.gss.redhat_support_lib.infrastructure.Solutions;
import com.redhat.gss.redhat_support_lib.infrastructure.Groups;
import com.redhat.gss.redhat_support_lib.infrastructure.Symptoms;
import com.redhat.gss.redhat_support_lib.web.ConnectionManager;

import javax.ws.rs.core.Cookie;

public class API {
    ConfigHelper config = null;
    ConnectionManager connectionManager = null;

    Search search = null;
    Solutions solutions = null;
    Articles articles = null;
    Cases cases = null;
    Products products = null;
    Comments comments = null;
    Entitlements entitlements = null;
    Problems problems = null;
    Attachments attachments = null;
    Ping ping = null;
    Groups groups = null;
    Symptoms symptoms = null;

    /**
     * @param username
     *            Strata username
     * @param password
     *            Strata password
     * @param url
     *            URL for Strata, default is https://api.access.redhat.com
     * @param proxyUser
     *            Proxy server username
     * @param proxyPassword
     *            Proxy server password
     * @param proxyUrl
     *            URL for proxy server
     * @param proxyPort
     *            Proxy server port
     * @param userAgent
     *            User agent string
     */
    public API(String username, String password, String url, String proxyUser,
            String proxyPassword, URL proxyUrl, int proxyPort, String userAgent) {
        config = new ConfigHelper(username, password, url, proxyUser,
                proxyPassword, proxyUrl, proxyPort, userAgent, false);
        connectionManager = new ConnectionManager(config);

        search = new Search(connectionManager);
        solutions = new Solutions(connectionManager);
        articles = new Articles(connectionManager);
        cases = new Cases(connectionManager);
        products = new Products(connectionManager);
        comments = new Comments(connectionManager);
        entitlements = new Entitlements(connectionManager);
        problems = new Problems(connectionManager);
        attachments = new Attachments(connectionManager);
        ping = new Ping(connectionManager);
        groups = new Groups(connectionManager);
        symptoms = new Symptoms(connectionManager);
    }

    /**
     * @param username
     *            Strata username
     * @param password
     *            Strata password
     * @param url
     *            URL for Strata, default is https://api.access.redhat.com
     * @param proxyUser
     *            Proxy server username
     * @param proxyPassword
     *            Proxy server password
     * @param proxyUrl
     *            URL for proxy server
     * @param proxyPort
     *            Proxy server port
     * @param userAgent
     *            User agent string
     * @param isDevel
     *            Only true if debugging for development
     */
    public API(String username, String password, String url, String proxyUser,
            String proxyPassword, URL proxyUrl, int proxyPort,
            String userAgent, boolean isDevel) {
        config = new ConfigHelper(username, password, url, proxyUser,
                proxyPassword, proxyUrl, proxyPort, userAgent, isDevel);
        connectionManager = new ConnectionManager(config);

        search = new Search(connectionManager);
        solutions = new Solutions(connectionManager);
        articles = new Articles(connectionManager);
        cases = new Cases(connectionManager);
        products = new Products(connectionManager);
        comments = new Comments(connectionManager);
        entitlements = new Entitlements(connectionManager);
        problems = new Problems(connectionManager);
        attachments = new Attachments(connectionManager);
        ping = new Ping(connectionManager);
        groups = new Groups(connectionManager);
        symptoms = new Symptoms(connectionManager);
    }

    /**
     * @param username
     *            Strata username
     * @param password
     *            Strata password
     * @param url
     *            URL for Strata, default is https://api.access.redhat.com
     * @param proxyUser
     *            Proxy server username
     * @param proxyPassword
     *            Proxy server password
     * @param proxyUrl
     *            URL for proxy server
     * @param proxyPort
     *            Proxy server port
     * @param userAgent
     *            User agent string
     * @param connectionTimeout
     *            Connection Timeout
     * @param isDevel
     *            Only true if debugging for development
     */
    public API(String username, String password, String url, String proxyUser,
            String proxyPassword, URL proxyUrl, int proxyPort,
            String userAgent, int connectionTimeout, boolean isDevel) {
        config = new ConfigHelper(username, password, url, proxyUser,
                proxyPassword, proxyUrl, proxyPort, userAgent,
                connectionTimeout, isDevel);
        connectionManager = new ConnectionManager(config);

        search = new Search(connectionManager);
        solutions = new Solutions(connectionManager);
        articles = new Articles(connectionManager);
        cases = new Cases(connectionManager);
        products = new Products(connectionManager);
        comments = new Comments(connectionManager);
        entitlements = new Entitlements(connectionManager);
        problems = new Problems(connectionManager);
        attachments = new Attachments(connectionManager);
        ping = new Ping(connectionManager);
        groups = new Groups(connectionManager);
        symptoms = new Symptoms(connectionManager);
    }

    public API(String url, String proxyUser, String proxyPassword,
            URL proxyUrl, int proxyPort, String userAgent,
            Map<String, Cookie> cookies, boolean isDevel) {
        config = new ConfigHelper(url, proxyUser, proxyPassword, proxyUrl,
                proxyPort, userAgent, cookies, isDevel);
        connectionManager = new ConnectionManager(config);

        search = new Search(connectionManager);
        solutions = new Solutions(connectionManager);
        articles = new Articles(connectionManager);
        cases = new Cases(connectionManager);
        products = new Products(connectionManager);
        comments = new Comments(connectionManager);
        entitlements = new Entitlements(connectionManager);
        problems = new Problems(connectionManager);
        attachments = new Attachments(connectionManager);
        ping = new Ping(connectionManager);
        groups = new Groups(connectionManager);
        symptoms = new Symptoms(connectionManager);
    }

    public API(String username, String password, String url,
            String proxyUsername, String proxyPassword, URL hostUrl,
            int proxyPort, String ftpHost, int ftpPort, String ftpUsername,
            String ftpPassword, boolean devel) {
        config = new ConfigHelper(username, password, url, proxyUsername,
                proxyPassword, hostUrl, proxyPort, ftpHost, ftpPort,
                ftpUsername, ftpPassword, devel);
        connectionManager = new ConnectionManager(config);

        search = new Search(connectionManager);
        solutions = new Solutions(connectionManager);
        articles = new Articles(connectionManager);
        cases = new Cases(connectionManager);
        products = new Products(connectionManager);
        comments = new Comments(connectionManager);
        entitlements = new Entitlements(connectionManager);
        problems = new Problems(connectionManager);
        attachments = new Attachments(connectionManager);
        ping = new Ping(connectionManager);
        groups = new Groups(connectionManager);
        symptoms = new Symptoms(connectionManager);
    }

    public API(String configFileName) throws IOException {
        config = new ConfigHelper(configFileName);
        connectionManager = new ConnectionManager(config);

        search = new Search(connectionManager);
        solutions = new Solutions(connectionManager);
        articles = new Articles(connectionManager);
        cases = new Cases(connectionManager);
        products = new Products(connectionManager);
        comments = new Comments(connectionManager);
        entitlements = new Entitlements(connectionManager);
        problems = new Problems(connectionManager);
        attachments = new Attachments(connectionManager);
        ping = new Ping(connectionManager);
        groups = new Groups(connectionManager);
        symptoms = new Symptoms(connectionManager);
    }

    public Search getSearch() {
        return search;
    }

    public Solutions getSolutions() {
        return solutions;
    }

    public Articles getArticles() {
        return articles;
    }

    public Cases getCases() {
        return cases;
    }

    public Products getProducts() {
        return products;
    }

    public Comments getComments() {
        return comments;
    }

    public Entitlements getEntitlements() {
        return entitlements;
    }

    public Problems getProblems() {
        return problems;
    }

    public Attachments getAttachments() {
        return attachments;
    }

    public ConfigHelper getConfigHelper() {
        return config;
    }

    public Ping getPing() {
        return ping;
    }

    public Groups getGroups() {
        return groups;
    }

    public Symptoms getSymptoms() {
        return symptoms;
    }
}
