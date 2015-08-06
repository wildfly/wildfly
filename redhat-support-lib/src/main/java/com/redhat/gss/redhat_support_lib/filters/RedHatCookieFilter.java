package com.redhat.gss.redhat_support_lib.filters;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Cookie;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * User: akovari Date: 7/23/14 Time: 12:09 PM
 */
public class RedHatCookieFilter implements ClientRequestFilter {
    Map<String, Cookie> cookies;

    public RedHatCookieFilter(Map<String, Cookie> cookies) {
        this.cookies = cookies;
    }

    public void filter(ClientRequestContext clientRequestContext)
            throws IOException {
        if (this.cookies != null) {
            List<Object> cookies = new ArrayList<Object>(this.cookies.values());
            clientRequestContext.getHeaders().put("Cookie", cookies);
        }
    }
}
