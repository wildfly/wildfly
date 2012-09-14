/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.osgi.xservice.bundle;

import java.util.Dictionary;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

/**
 * A simple managed service
 *
 * @author Thomas.Diesler@jboss.org
 * @since 12-Dec-2010
 */
public class ConfiguredService implements ManagedService {

    public static String SERVICE_PID = ConfiguredService.class.getName();

    @SuppressWarnings("rawtypes")
    private Dictionary properties;
    private CountDownLatch latch = new CountDownLatch(1);

    @Override
    @SuppressWarnings("rawtypes")
    public void updated(Dictionary update) throws ConfigurationException {
        properties = update;
        if (latch.getCount() == 1)
            latch.countDown();
    }

    public String getValue(String key) throws InterruptedException, TimeoutException {

        // Wait a little for the update to happen
        if (latch.await(5, TimeUnit.SECONDS) == false)
            throw new TimeoutException();

        return (String) properties.get(key);
    }

}
