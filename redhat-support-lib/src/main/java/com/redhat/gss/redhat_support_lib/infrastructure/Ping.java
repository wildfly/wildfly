package com.redhat.gss.redhat_support_lib.infrastructure;

import java.net.MalformedURLException;

import com.redhat.gss.redhat_support_lib.errors.RequestException;
import com.redhat.gss.redhat_support_lib.web.ConnectionManager;

public class Ping extends BaseQuery {
    private ConnectionManager connectionManager = null;
    static String url = "/rs/";

    public Ping(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * Ping a connection
     *
     * @return A string object.
     * @throws RequestException
     *             An exception if there was a connection related issue.
     * @throws MalformedURLException
     */
    public String ping() throws RequestException, MalformedURLException {
        String fullUrl = connectionManager.getConfig().getUrl() + url;
        return get(connectionManager.getConnection(), fullUrl, String.class);
    }
}
