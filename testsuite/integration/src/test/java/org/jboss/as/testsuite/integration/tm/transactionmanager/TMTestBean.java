/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.testsuite.integration.tm.transactionmanager;

import org.jboss.logging.Logger;
import org.jboss.util.NestedException;

/**
 * Server Side TM test.
 * Note: This used to be a MBean in AS6, now it is a POJO.
 *
 * @author adrian@jboss.org
 * @author istudens@redhat.com
 */
public class TMTestBean {
    private static final Logger log = Logger.getLogger(TMTestBean.class);

    public void testOperations(String test, Operation[] ops) throws Exception {
        log.info("Starting test " + test);
        Operation.start(log);
        int i = 0;
        try {
            for (; i < ops.length; ++i)
                ops[i].perform();
        } catch (Exception e) {
            throw new NestedException(test + " operation " + i, e);
        } finally {
            log.info("Finished test " + test);
            try {
                Operation.end();
            } finally {
                Operation.tidyUp();
            }
        }
    }
}
