/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.context.application.lifecycle;

import org.jboss.logging.Logger;

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.BeforeDestroyed;
import jakarta.enterprise.context.Destroyed;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.context.control.RequestContextController;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.jms.JMSContext;
import jakarta.jms.Queue;
import javax.naming.NamingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author emmartins
 */
@WebServlet(name="Servlet", urlPatterns={"/FiveHundred"})
public class Servlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(Servlet.class.getName());

    @Resource
    private ManagedExecutorService executorService;

    @Inject
    private JMSContext jmsContext;

    @Resource(lookup = Mdb.JNDI_NAME)
    private Queue jmsQueue;

    @Inject
    private RequestContextController requestContextController;

    @EJB(lookup = "java:global/TEST_RESULTS/TestResultsBean!org.jboss.as.test.integration.weld.context.application.lifecycle.TestResults")
    private TestResults testResults;

    public void onInitialized(@Observes @Initialized(ApplicationScoped.class) Object event) {
        LOGGER.info("onInitialized!");
        try {
            Utils.sendMessage(jmsContext, jmsQueue, requestContextController);
            Utils.lookupCommonResources(LOGGER, false);
            executorService.submit(() -> {
                try {
                    Utils.sendMessage(jmsContext, jmsQueue, requestContextController);
                    Utils.lookupCommonResources(LOGGER, false);
                    testResults.setServletInitialized(true);
                } catch (NamingException e) {
                    LOGGER.error("Failed to set initialized test result", e);
                    testResults.setServletInitialized(false);
                }
            }).get();
        } catch (Throwable e) {
            LOGGER.error("Failed to set initialized test result", e);
            testResults.setServletInitialized(false);
        }
    }

    public void onBeforeDestroyed(@Observes @BeforeDestroyed(ApplicationScoped.class) Object object) {
        LOGGER.info("onBeforeDestroyed!");
        try {
            Utils.sendMessage(jmsContext, jmsQueue, requestContextController);
            Utils.lookupCommonResources(LOGGER, false);
            executorService.submit(() -> {
                try {
                    Utils.sendMessage(jmsContext, jmsQueue, requestContextController);
                    Utils.lookupCommonResources(LOGGER, false);
                    testResults.setServletBeforeDestroyed(true);
                } catch (NamingException e) {
                    LOGGER.error("Failed to set before destroyed test result", e);
                    testResults.setServletBeforeDestroyed(false);
                }
            }).get();
        } catch (Throwable e) {
            LOGGER.error("Failed to set before destroyed test result", e);
            testResults.setServletBeforeDestroyed(false);
        }
    }

    public void onDestroyed(@Observes @Destroyed(ApplicationScoped.class) Object object) {
        LOGGER.info("onDestroyed!");
        try {
            executorService.submit(() -> testResults.setServletDestroyed(true)).get();
        } catch (Throwable e) {
            LOGGER.error("Failed to set destroyed test result", e);
            testResults.setServletDestroyed(false);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.sendError(500);
    }
}
