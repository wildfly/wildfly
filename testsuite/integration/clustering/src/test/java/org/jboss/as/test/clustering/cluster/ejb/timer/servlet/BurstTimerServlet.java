/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2024, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.clustering.cluster.ejb.timer.servlet;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;

import jakarta.servlet.annotation.WebServlet;

import org.jboss.as.test.clustering.cluster.ejb.timer.beans.BurstPersistentTimerBean;

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
        super(Set.of(BurstPersistentTimerBean.class), Set.of());
    }
}
