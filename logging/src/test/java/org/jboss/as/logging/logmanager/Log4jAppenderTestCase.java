/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging.logmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;
import org.jboss.logging.Logger;
import org.jboss.logmanager.formatters.PatternFormatter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class Log4jAppenderTestCase {

    private static final String CATEGORY = Log4jAppenderTestCase.class.getName();
    private static final String DFT_MESSAGE = "This is a test message";

    private final TestAppender appender = TestAppender.INSTANCE;
    private final Log4jAppenderHandler handler = new Log4jAppenderHandler(appender, true);
    private final org.jboss.logmanager.Logger lmLogger = org.jboss.logmanager.Logger.getLogger(CATEGORY);
    private final Logger logger = Logger.getLogger(CATEGORY);

    @Before
    public void setUp() {
        handler.setAppender(appender);
        handler.setLevel(Level.INFO);
        lmLogger.addHandler(handler);
        lmLogger.setLevel(Level.INFO);
    }

    @After
    public void tearDown() {
        appender.close();
        handler.close();
        lmLogger.removeHandler(handler);
    }


    @Test
    public void formatTest() throws Exception {
        final String pattern = "fmt: %s";
        final PatternFormatter formatter = new PatternFormatter(pattern);
        handler.setFormatter(formatter);
        logger.info(DFT_MESSAGE);
        Assert.assertEquals(String.format(pattern, DFT_MESSAGE), appender.messages.get(0));
        logger.infof("%s", DFT_MESSAGE);
        Assert.assertEquals(String.format(pattern, DFT_MESSAGE), appender.messages.get(1));
    }

    @Test
    public void levelTest() throws Exception {
        lmLogger.setLevel(Level.ALL);

        // Should be 3 messages
        handler.setLevel(Level.ALL);
        logger.info(DFT_MESSAGE);
        logger.error(DFT_MESSAGE);
        logger.trace(DFT_MESSAGE);
        Assert.assertEquals(3, appender.counter);

        // Should be 2 messages
        appender.close();
        handler.setLevel(Level.INFO);
        logger.info(DFT_MESSAGE);
        logger.error(DFT_MESSAGE);
        logger.trace(DFT_MESSAGE);
        Assert.assertEquals(2, appender.counter);

        // Should be 0 messages
        appender.close();
        handler.setLevel(Level.OFF);
        logger.info(DFT_MESSAGE);
        logger.error(DFT_MESSAGE);
        logger.trace(DFT_MESSAGE);
        Assert.assertEquals(0, appender.counter);
    }

    static class TestAppender extends AppenderSkeleton {
        static TestAppender INSTANCE = new TestAppender();
        int counter = 0;
        final List<String> messages = new ArrayList<String>();

        @Override
        protected void append(final LoggingEvent event) {
            counter++;
            final Layout layout = getLayout();
            if (layout != null) {
                messages.add(layout.format(event));
            } else {
                messages.add(event.getLogRecord().getFormattedMessage());
            }
        }

        @Override
        public void close() {
            counter = 0;
            messages.clear();
        }

        @Override
        public boolean requiresLayout() {
            return false;
        }
    }
}
