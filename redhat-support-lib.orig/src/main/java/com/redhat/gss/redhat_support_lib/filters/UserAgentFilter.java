package com.redhat.gss.redhat_support_lib.filters;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;

public class UserAgentFilter implements ClientRequestFilter {
    String userAgent = null;

    public UserAgentFilter(String userAgent) {
        this.userAgent = userAgent;
    }

    public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().add(HttpHeaders.USER_AGENT, userAgent);
    }
}
