/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.timer.servlet;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;

import jakarta.servlet.annotation.WebServlet;

import org.jboss.as.test.clustering.cluster.ejb.timer.beans.BurstPersistentCalendarTimerBean;
import org.jboss.as.test.clustering.cluster.ejb.timer.beans.BurstPersistentIntervalTimerBean;

/**
 * @author Paul Ferraro
 */
@WebServlet(urlPatterns = { BurstTimerServlet.SERVLET_PATH })
public class BurstTimerServlet extends AbstractTimerServlet {
    private static final long serialVersionUID = 6726244990295355667L;
    private static final String SERVLET_NAME = "burst-timer";
    static final String SERVLET_PATH = "/" + SERVLET_NAME;

    public static URI createURI(URL baseURL, String module) throws URISyntaxException {
        return baseURL.toURI().resolve(new StringBuilder(SERVLET_NAME).append('?').append(MODULE).append('=').append(module).toString());
    }

    public BurstTimerServlet() {
        super(Set.of(BurstPersistentCalendarTimerBean.class, BurstPersistentIntervalTimerBean.class), Set.of());
    }
}
