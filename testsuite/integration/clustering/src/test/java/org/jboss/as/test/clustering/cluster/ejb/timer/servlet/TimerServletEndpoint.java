/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.timer.servlet;

import java.io.IOException;

import org.jboss.as.test.clustering.cluster.ejb.timer.beans.TimerServiceBean;
import org.jboss.logging.Logger;

import jakarta.ejb.EJB;
import jakarta.ejb.Timer;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = { "/" + TimerServletEndpoint.ENDPOINT })
public class TimerServletEndpoint extends HttpServlet {

    private static final Logger log = Logger.getLogger(TimerServletEndpoint.class);

    public static final String ENDPOINT = "TimerServlet";

    private static final long serialVersionUID = 1L;

    @EJB
    TimerServiceBean timerServiceBean;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String[] value = timerServiceBean.getTimers().stream().map(Timer::getInfo).map(String.class::cast).toArray(String[]::new);
            resp.getOutputStream().write(String.join("\n", value).getBytes());
            resp.getOutputStream().flush();
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            log.error("Error during get", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String content = new String(req.getInputStream().readAllBytes());
            timerServiceBean.createTimer(content);
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            log.error("Error during get", e);
        }
    }
}