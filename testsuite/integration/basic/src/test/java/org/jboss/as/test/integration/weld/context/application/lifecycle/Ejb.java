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

import jakarta.annotation.Resource;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
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

/**
 * @author emmartins
 */
@Stateless
public class Ejb {

    private static final Logger LOGGER = Logger.getLogger(Ejb.class.getName());

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
            Utils.lookupCommonResources(LOGGER, true);
            executorService.submit(() -> {
                try {
                    Utils.sendMessage(jmsContext, jmsQueue, requestContextController);
                    Utils.lookupCommonResources(LOGGER, true);
                    testResults.setEjbBeanInitialized(true);
                } catch (NamingException e) {
                    LOGGER.error("Failed to set initialized test result", e);
                    testResults.setEjbBeanInitialized(false);
                }
            }).get();
        } catch (Throwable e) {
            LOGGER.error("Failed to set initialized test result", e);
            testResults.setEjbBeanInitialized(false);
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
                    testResults.setEjbBeanBeforeDestroyed(true);
                } catch (NamingException e) {
                    LOGGER.error("Failed to set before destroyed test result", e);
                    testResults.setEjbBeanBeforeDestroyed(false);
                }
            }).get();
        } catch (Throwable e) {
            LOGGER.error("Failed to set before destroyed test result", e);
            testResults.setEjbBeanBeforeDestroyed(false);
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
                    testResults.setEjbBeanDestroyed(true);
                } catch (NamingException e) {
                    LOGGER.error("Failed to set destroyed test result", e);
                    testResults.setEjbBeanDestroyed(false);
                }
            }).get();
        } catch (Throwable e) {
            LOGGER.error("Failed to set destroyed test result", e);
            testResults.setEjbBeanDestroyed(false);
        }
    }
}
