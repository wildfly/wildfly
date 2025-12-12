/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.clustering.cluster.ejb.timer.app;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.jboss.logging.Logger;

import jakarta.ejb.EJB;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
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
            Jsonb jsonb = JsonbBuilder.newBuilder().build();
            List<TimerInfo> timers = timerServiceBean.getTimers();
            String outcome = jsonb.toJson(timers);
            resp.getOutputStream().write(outcome.getBytes());
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            log.error("error!", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Jsonb jsonb = JsonbBuilder.newBuilder().build();
        try {
            byte[] content = req.getInputStream().readAllBytes();
            InputStream is = new ByteArrayInputStream(content);
            TimerInfo timerInfo = jsonb.fromJson(is, TimerInfo.class);
            timerServiceBean.createTimer(timerInfo);
            resp.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            log.error("error!", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

    }
}
