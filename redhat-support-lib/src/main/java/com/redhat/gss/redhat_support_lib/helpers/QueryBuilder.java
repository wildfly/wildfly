package com.redhat.gss.redhat_support_lib.helpers;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import org.jboss.logging.Logger;

public class QueryBuilder {

    private static final Logger LOGGER = Logger.getLogger(QueryBuilder.class
            .getName());

    public static String appendQuery(String url, List<String> qargs) {
        // Appends url params to url

        String query_params = new String();

        if (qargs != null && qargs.size() > 0) {
            for (String qarg : qargs) {
                String[] splitArg = qarg.split("=");
                if (splitArg != null && splitArg.length == 2) {
                    if (!query_params.contains("?")) {
                        try {
                            query_params += "?" + splitArg[0] + "="
                                    + URLEncoder.encode(splitArg[1], "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            LOGGER.warn(e.getMessage());
                        }
                        // query_params += '?' + arg;
                    } else {
                        try {
                            query_params += "&" + splitArg[0] + "="
                                    + URLEncoder.encode(splitArg[1], "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            LOGGER.warn(e.getMessage());
                        }
                        // query_params += '&' + arg;
                    }
                }
            }
        }
        return (url + query_params);
    }
}