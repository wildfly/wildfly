/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.ejb3.timerservice;

import org.jboss.ejb3.timerservice.mk2.TimerServiceFactoryImpl;
import org.jboss.ejb3.timerservice.mk2.persistence.filestore.FileTimerPersistence;
import org.jboss.ejb3.timerservice.spi.TimerServiceFactory;
import org.jboss.modules.Module;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Service that manages the lifecycle of a TimerServiceFactory
 *
 * @author Stuart Douglas
 */
public class TimerServiceFactoryService implements Service<TimerServiceFactory> {

    public static final ServiceName SERVICE_NAME = ServiceName.of("ejb", "timerServiceFactory");


    public static final ServiceName PATH_SERVICE_NAME = ServiceName.JBOSS.append("as", "ejb", "timerServiceFactory", "dataStorePath");


    private volatile TimerServiceFactory timerServiceFactory;
    private volatile ExecutorService executorService;
    private volatile FileTimerPersistence timerPersistence;

    private final InjectedValue<TransactionManager> transactionManagerInjectedValue = new InjectedValue<TransactionManager>();
    private final InjectedValue<TransactionSynchronizationRegistry> transactionSynchronizationRegistryInjectedValue = new InjectedValue<TransactionSynchronizationRegistry>();
    private final InjectedValue<String> path = new InjectedValue<String>();
    private final int maxThreads;
    private final int coreThreads;
    private final String name;
    private final Module module;

    public TimerServiceFactoryService(final int coreThreads, final int maxThreads, final String name, final Module module) {
        this.name = name;
        this.coreThreads = coreThreads;
        this.maxThreads = maxThreads;
        this.module = module;
    }


    @Override
    public  void start(final StartContext context) throws StartException {

        executorService = new ThreadPoolExecutor(coreThreads, maxThreads, 1, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>());
        //only start the persistence service if it has been configured
        final String path = this.path.getOptionalValue();
        if (path != null) {
            timerPersistence = new FileTimerPersistence(transactionManagerInjectedValue.getValue(), transactionSynchronizationRegistryInjectedValue.getValue(), new File(path + File.separatorChar + name), true, module.getModuleLoader());
            timerPersistence.start();
        }
        timerServiceFactory = new TimerServiceFactoryImpl(timerPersistence, transactionManagerInjectedValue.getValue(), executorService);
    }

    @Override
    public void stop(final StopContext context) {
        executorService.shutdownNow();
        if (path == null) {
            timerPersistence.stop();
            timerPersistence = null;
        }
        timerServiceFactory = null;
        executorService = null;
    }

    @Override
    public TimerServiceFactory getValue() throws IllegalStateException, IllegalArgumentException {
        return timerServiceFactory;
    }

    public TimerServiceFactory getTimerServiceFactory() {
        return timerServiceFactory;
    }

    public InjectedValue<TransactionManager> getTransactionManagerInjectedValue() {
        return transactionManagerInjectedValue;
    }

    public InjectedValue<TransactionSynchronizationRegistry> getTransactionSynchronizationRegistryInjectedValue() {
        return transactionSynchronizationRegistryInjectedValue;
    }

    public InjectedValue<String> getPath() {
        return path;
    }
}
