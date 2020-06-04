/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ejb.management.deployments;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import javax.ejb.AccessTimeout;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.LocalBean;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;

import org.jboss.logging.Logger;

@Singleton
@TransactionManagement(TransactionManagementType.BEAN)
@AccessTimeout(value = 1, unit = TimeUnit.SECONDS)
@Remote(BusinessInterface.class)
@LocalBean
public class WaitTimeSingletonBean implements BusinessInterface {
    private static final Logger logger = Logger.getLogger(WaitTimeSingletonBean.class);

    @Resource
    private SessionContext sessionContext;

    @Asynchronous
    public Future<Long> async() {
        return new AsyncResult<>(System.currentTimeMillis());
    }

    @Override
    public void doIt() {
        final WaitTimeSingletonBean bean = sessionContext.getBusinessObject(WaitTimeSingletonBean.class);
        final Future<Long> future = bean.async();
        try {
            future.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.logf(Logger.Level.INFO, "WaitTimeSingletonBean calling its own async method caused expected exception %s%n", e.getMessage());
        }
    }

    @Override
    public void remove() {
    }
}
