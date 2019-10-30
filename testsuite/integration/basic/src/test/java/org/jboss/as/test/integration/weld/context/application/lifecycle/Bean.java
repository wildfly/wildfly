/*
 * JBoss, Home of Professional Open Source
 * Copyright 2019, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.jboss.as.test.integration.weld.context.application.lifecycle;

import org.jboss.logging.Logger;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.context.control.RequestContextController;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.Queue;
import javax.naming.NamingException;

/**
 * @author emmartins
 */
@ApplicationScoped
public class Bean {

    private static final Logger LOGGER = Logger.getLogger(Bean.class.getName());

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

    public void onInitialized(@Observes @Initialized(ApplicationScoped.class) Object object) {
        LOGGER.info("onInitialized!");
        try {
            Utils.sendMessage(jmsContext, jmsQueue, requestContextController);
            Utils.lookupCommonResources(LOGGER, false);
            executorService.submit(() -> {
                try {
                    Utils.sendMessage(jmsContext, jmsQueue, requestContextController);
                    Utils.lookupCommonResources(LOGGER, false);
                    testResults.setCdiBeanInitialized(true);
                } catch (NamingException e) {
                    LOGGER.error("Failed to set initialized test result", e);
                    testResults.setCdiBeanInitialized(false);
                }
            }).get();
        } catch (Throwable e) {
            LOGGER.error("Failed to set initialized test result", e);
            testResults.setCdiBeanInitialized(false);
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
                    testResults.setCdiBeanBeforeDestroyed(true);
                } catch (NamingException e) {
                    LOGGER.error("Failed to set initialized test result", e);
                    testResults.setCdiBeanBeforeDestroyed(false);
                }
            }).get();
        } catch (Throwable e) {
            LOGGER.error("Failed to set initialized test result", e);
            testResults.setCdiBeanBeforeDestroyed(false);
        }
    }

    public void onDestroyed(@Observes @Destroyed(ApplicationScoped.class) Object object) {
        LOGGER.info("onDestroyed!");
        try {
            Utils.sendMessage(jmsContext, jmsQueue, requestContextController);
            Utils.lookupCommonResources(LOGGER, false);
            executorService.submit(() -> {
                try {
                    Utils.sendMessage(jmsContext, jmsQueue, requestContextController);
                    Utils.lookupCommonResources(LOGGER, false);
                    testResults.setCdiBeanDestroyed(true);
                } catch (NamingException e) {
                    LOGGER.error("Failed to set initialized test result", e);
                    testResults.setCdiBeanDestroyed(false);
                }
            }).get();
        } catch (Throwable e) {
            LOGGER.error("Failed to set initialized test result", e);
            testResults.setCdiBeanDestroyed(false);
        }
    }
}
