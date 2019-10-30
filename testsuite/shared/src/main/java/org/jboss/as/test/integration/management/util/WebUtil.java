/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jboss.as.test.integration.management.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;
import org.jboss.as.test.integration.common.HttpRequest;

/**
 * @author dpospisi
 */
public class WebUtil {
    public static boolean testHttpURL(String url) {
        boolean failed = false;
        try {
            HttpRequest.get(url, 10, TimeUnit.SECONDS);
        } catch (Exception e) {
            failed = true;
        }
        return !failed;

    }

    public static String getBaseURL(URL url) throws MalformedURLException {
        return new URL(url.getProtocol(), url.getHost(), url.getPort(), "/").toString();
    }
}
