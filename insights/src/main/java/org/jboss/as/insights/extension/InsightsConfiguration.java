/*
 * Copyright (C) 2015 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.insights.extension;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2015 Red Hat, inc.
 */
public class InsightsConfiguration {

    /**
     * Endpoint of url which when added to the end of the url reveals the location of where to send the JDR Should take
     * the format of /insights/endpoint/
     */
    private final String insightsEndpoint;

    /**
     * Endpoint of url which when added to the end of the url reveals the location of where to query for the current
     * system uuid
     */
    private final String systemEndpoint;
    private final String rhnUid;
    private final String rhnPw;
    private final String proxyUrl;
    private final int proxyPort;
    private final String proxyUser;
    private final String proxyPw;
    private final String url;
    private final String userAgent;
    private final int scheduleInterval;

    public InsightsConfiguration(String insightsEndpoint, String systemEndpoint, String rhnUid, String rhnPw, String proxyUrl, int proxyPort, String proxyUser, String proxyPw, String url, String userAgent, int scheduleInterval) {
        this.insightsEndpoint = insightsEndpoint;
        this.systemEndpoint = systemEndpoint;
        this.rhnUid = rhnUid;
        this.rhnPw = rhnPw;
        this.proxyUrl = proxyUrl;
        this.proxyPort = proxyPort;
        this.proxyUser = proxyUser;
        this.proxyPw = proxyPw;
        this.url = url;
        this.userAgent = userAgent;
        this.scheduleInterval = scheduleInterval;
    }

    public int getScheduleInterval() {
        return scheduleInterval;
    }

    public String getInsightsEndpoint() {
        return insightsEndpoint;
    }

    public String getSystemEndpoint() {
        return systemEndpoint;
    }

    public String getRhnUid() {
        return rhnUid;
    }

    public String getRhnPw() {
        return rhnPw;
    }

    public String getProxyUrl() {
        return proxyUrl;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public String getProxyUser() {
        return proxyUser;
    }

    public String getProxyPw() {
        return proxyPw;
    }

    public String getUrl() {
        return url;
    }

    public String getUserAgent() {
        return userAgent;
    }

}
