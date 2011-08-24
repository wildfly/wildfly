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
package org.jboss.as.ejb3.timerservice.mk2.persistence.filestore;

import org.jboss.as.ejb3.timerservice.mk2.persistence.TimerEntity;
import org.jboss.as.ejb3.timerservice.mk2.persistence.TimerPersistence;
import org.jboss.logging.Logger;
import org.jboss.marshalling.InputStreamByteInput;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.marshalling.OutputStreamByteOutput;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.river.RiverMarshallerFactory;
import org.jboss.modules.ModuleLoader;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * File based persistent timer store.
 * <p/>
 * TODO: this is fairly hackey at the moment, it should be registered as an XA resource to support proper XA semantics
 *
 * @author Stuart Douglas
 */
public class FileTimerPersistence implements TimerPersistence {

    private final TransactionManager transactionManager;
    private final TransactionSynchronizationRegistry transactionSynchronizationRegistry;
    private final File baseDir;
    private final boolean createIfNotExists;
    private static final Logger logger = Logger.getLogger(FileTimerPersistence.class);
    private final MarshallerFactory factory;
    private final MarshallingConfiguration configuration;

    /**
     * map of timed object id : timer id : timer
     */
    private final ConcurrentMap<String, Map<String, TimerEntity>> timers = new ConcurrentHashMap<String, Map<String, TimerEntity>>();
    private final ConcurrentMap<String, Lock> locks = new ConcurrentHashMap<String, Lock>();
    private final ConcurrentMap<String, String> directories = new ConcurrentHashMap<String, String>();

    private volatile boolean started = false;

    public FileTimerPersistence(final TransactionManager transactionManager, final TransactionSynchronizationRegistry transactionSynchronizationRegistry, final File baseDir, final boolean createIfNotExists, final ModuleLoader moduleLoader) {
        this.transactionManager = transactionManager;
        this.transactionSynchronizationRegistry = transactionSynchronizationRegistry;
        this.baseDir = baseDir;
        this.createIfNotExists = createIfNotExists;
        RiverMarshallerFactory factory = new RiverMarshallerFactory();
        MarshallingConfiguration configuration = new MarshallingConfiguration();
        configuration.setClassResolver(ModularClassResolver.getInstance(moduleLoader));

        this.configuration = configuration;
        this.factory = factory;
    }

    @Override
    public synchronized void start() {
        started = true;
        if (!baseDir.exists()) {
            if (createIfNotExists) {
                if (!baseDir.mkdirs()) {
                    throw new RuntimeException("Could not create timer file store directory " + baseDir);
                }
            } else {
                throw new RuntimeException("Timer file store directory " + baseDir + " does not exist");
            }
        }
        if (!baseDir.isDirectory()) {
            throw new RuntimeException("Timer file store directory " + baseDir + " is not a directory");
        }
    }

    @Override
    public synchronized void stop() {
        timers.clear();
        started = false;
    }


    @Override
    public void persistTimer(final TimerEntity timerEntity) {
        final Lock lock = getLock(timerEntity.getTimedObjectId());
        try {
            final int status = transactionManager.getStatus();
            if (status == Status.STATUS_NO_TRANSACTION ||
                    status == Status.STATUS_UNKNOWN) {
                try {
                    lock.lock();
                    Map<String, TimerEntity> map = getTimers(timerEntity.getTimedObjectId());
                    map.put(timerEntity.getId(), timerEntity);
                    writeFile(timerEntity);
                } finally {
                    lock.unlock();
                }
            } else {
                transactionSynchronizationRegistry.registerInterposedSynchronization(new PersistTransactionSynchronization(timerEntity, lock));
            }
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public TimerEntity loadTimer(final String id, final String timedObjectId) {
        final Lock lock = getLock(timedObjectId);
        try {
            lock.lock();
            final Map<String, TimerEntity> timers = getTimers(timedObjectId);
            return timers.get(id);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void removeTimer(final TimerEntity timerEntity) {
        final Lock lock = getLock(timerEntity.getTimedObjectId());
        try {
            lock.lock();
            //remove is not a transactional operation, as it only happens once the timer has expired
            final Map<String, TimerEntity> timers = getTimers(timerEntity.getTimedObjectId());
            timers.remove(timerEntity.getId());
            File file = fileName(timerEntity.getTimedObjectId(), timerEntity.getId());
            if (file.exists()) {
                if (!file.delete()) {
                    logger.error("Could not remove persistent timer " + file);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<TimerEntity> loadActiveTimers(final String timedObjectId) {
        final Lock lock = getLock(timedObjectId);
        try {
            lock.lock();
            final Map<String, TimerEntity> timers = getTimers(timedObjectId);
            return new ArrayList<TimerEntity>(timers.values());
        } finally {
            lock.unlock();
        }
    }

    private Lock getLock(final String timedObjectId) {
        Lock lock = locks.get(timedObjectId);
        if (lock == null) {
            final Lock addedLock = new ReentrantLock();
            lock = locks.putIfAbsent(timedObjectId, addedLock);
            if (lock == null) {
                lock = addedLock;
            }
        }
        return lock;
    }

    /**
     * Gets the timer map, loading from the persistent store if necessary. Should be called under lock
     *
     * @param timedObjectId The timed object id
     * @return The timers for the object
     */
    private Map<String, TimerEntity> getTimers(final String timedObjectId) {
        Map<String, TimerEntity> map = timers.get(timedObjectId);
        if (map == null) {
            map = loadTimersFromFile(timedObjectId);
            timers.put(timedObjectId, map);
        }
        return map;
    }

    private Map<String, TimerEntity> loadTimersFromFile(final String timedObjectId) {
        final Map<String, TimerEntity> timers = new HashMap<String, TimerEntity>();
        try {
            final File file = new File(getDirectory(timedObjectId));
            if (!file.exists()) {
                //no timers exist yet
                return timers;
            } else if (!file.isDirectory()) {
                logger.error(file + " is not a directory, could not restore timers");
                return timers;
            }
            Unmarshaller unmarshaller = factory.createUnmarshaller(configuration);
            for (File timerFile : file.listFiles()) {
                FileInputStream in = null;
                try {
                    in = new FileInputStream(timerFile);
                    unmarshaller.start(new InputStreamByteInput(in));
                    final TimerEntity entity = unmarshaller.readObject(TimerEntity.class);
                    timers.put(entity.getId(), entity);
                    unmarshaller.finish();
                } catch (Exception e) {
                    logger.error("Could not restore timer from " + timerFile, e);
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            logger.error("error closing file ", e);
                        }
                    }
                }

            }
        } catch (Exception e) {
            logger.error("Could not restore timers for " + timedObjectId, e);
        }
        return timers;
    }

    private File fileName(String timedObjectId, String timerId) {
        return new File(getDirectory(timedObjectId) + File.separator + timerId.replace(File.separator, "-"));
    }

    /**
     * Gets the directory for a given timed object, making sure it exists.
     *
     * @param timedObjectId The timed object
     * @return The directory
     */
    private String getDirectory(String timedObjectId) {
        String dirName = directories.get(timedObjectId);
        if (dirName == null) {
            dirName = baseDir.getAbsolutePath() + File.separator + timedObjectId.replace(File.separator, "-");
            File file = new File(dirName);
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    logger.error("Could not create directory " + file + " to persist EJB timers.");
                }
            }
            directories.put(timedObjectId, dirName);
        }
        return dirName;
    }


    private void writeFile(TimerEntity entity) {
        //write out our temporary file
        final File file = fileName(entity.getTimedObjectId(), entity.getId());

        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(file, false);
            final Marshaller marshaller = factory.createMarshaller(configuration);
            marshaller.start(new OutputStreamByteOutput(fileOutputStream));
            marshaller.writeObject(entity);
            marshaller.finish();
            fileOutputStream.flush();
            fileOutputStream.getFD().sync();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    logger.error("IOException closing file ", e);
                }
            }
        }
    }

    private final class PersistTransactionSynchronization implements Synchronization {

        private final TimerEntity timer;
        private final Lock lock;

        public PersistTransactionSynchronization(final TimerEntity timer, final Lock lock) {
            this.timer = timer;
            this.lock = lock;
        }

        @Override
        public void beforeCompletion() {

        }

        @Override
        public void afterCompletion(final int status) {
            try {
                lock.lock();
                if (status == Status.STATUS_COMMITTED) {
                    Map<String, TimerEntity> map = getTimers(timer.getTimedObjectId());
                    map.put(timer.getId(), timer);
                    writeFile(timer);
                }
            } finally {
                lock.unlock();
            }
        }


    }
}
