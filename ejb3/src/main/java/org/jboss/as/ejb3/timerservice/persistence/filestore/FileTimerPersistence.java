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
package org.jboss.as.ejb3.timerservice.persistence.filestore;

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

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.as.ejb3.component.stateful.CurrentSynchronizationCallback;
import org.jboss.as.ejb3.timerservice.TimerState;
import org.jboss.as.ejb3.timerservice.persistence.TimerEntity;
import org.jboss.as.ejb3.timerservice.persistence.TimerPersistence;
import org.jboss.marshalling.InputStreamByteInput;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.marshalling.OutputStreamByteOutput;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.marshalling.river.RiverMarshallerFactory;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import static org.jboss.as.ejb3.EjbMessages.MESSAGES;
import static org.jboss.as.ejb3.EjbLogger.ROOT_LOGGER;

/**
 * File based persistent timer store.
 * <p/>
 * TODO: this is fairly hackey at the moment, it should be registered as an XA resource to support proper XA semantics
 *
 * @author Stuart Douglas
 */
public class FileTimerPersistence implements TimerPersistence, Service<FileTimerPersistence> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "timerService", "fileTimerPersistence");

    private final boolean createIfNotExists;
    private MarshallerFactory factory;
    private MarshallingConfiguration configuration;
    private final InjectedValue<TransactionManager> transactionManager = new InjectedValue<TransactionManager>();
    private final InjectedValue<TransactionSynchronizationRegistry> transactionSynchronizationRegistry = new InjectedValue<TransactionSynchronizationRegistry>();
    private final InjectedValue<ModuleLoader> moduleLoader = new InjectedValue<ModuleLoader>();
    private final InjectedValue<String> baseDir = new InjectedValue<String>();


    /**
     * map of timed object id : timer id : timer
     */
    private final ConcurrentMap<String, Map<String, TimerEntity>> timers = new ConcurrentHashMap<String, Map<String, TimerEntity>>();
    private final ConcurrentMap<String, Lock> locks = new ConcurrentHashMap<String, Lock>();
    private final ConcurrentMap<String, String> directories = new ConcurrentHashMap<String, String>();

    private volatile boolean started = false;

    public FileTimerPersistence(final boolean createIfNotExists) {
        this.createIfNotExists = createIfNotExists;
    }

    @Override
    public synchronized void start(final StartContext context) {

        final RiverMarshallerFactory factory = new RiverMarshallerFactory();
        final MarshallingConfiguration configuration = new MarshallingConfiguration();
        configuration.setClassResolver(ModularClassResolver.getInstance(moduleLoader.getValue()));

        this.configuration = configuration;
        this.factory = factory;
        final File baseDir = new File(this.baseDir.getValue());
        if (!baseDir.exists()) {
            if (createIfNotExists) {
                if (!baseDir.mkdirs()) {
                    throw MESSAGES.failToCreateTimerFileStoreDir(baseDir);
                }
            } else {
                throw MESSAGES.timerFileStoreDirNotExist(baseDir);
            }
        }
        if (!baseDir.isDirectory()) {
            throw MESSAGES.invalidTimerFileStoreDir(baseDir);
        }
        started = true;
    }

    @Override
    public void stop(final StopContext context) {

        timers.clear();
        locks.clear();
        directories.clear();
        factory = null;
        configuration = null;
        started = false;
    }

    @Override
    public FileTimerPersistence getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void persistTimer(final TimerEntity timerEntity) {
        final Lock lock = getLock(timerEntity.getTimedObjectId());
        try {
            final int status = transactionManager.getValue().getStatus();
            if (status == Status.STATUS_MARKED_ROLLBACK || status == Status.STATUS_ROLLEDBACK ||
                    status == Status.STATUS_ROLLING_BACK) {
                //no need to persist anyway
                return;
            }
            if (status == Status.STATUS_NO_TRANSACTION ||
                    status == Status.STATUS_UNKNOWN || isBeforeCompletion()
                    || status == Status.STATUS_COMMITTED) {
                try {
                    lock.lock();
                    Map<String, TimerEntity> map = getTimers(timerEntity.getTimedObjectId());
                    if (timerEntity.getTimerState() == TimerState.CANCELED ||
                            timerEntity.getTimerState() == TimerState.EXPIRED) {
                        map.remove(timerEntity.getId());
                    } else {
                        map.put(timerEntity.getId(), timerEntity);
                    }
                    writeFile(timerEntity);
                } finally {
                    lock.unlock();
                }
            } else {

                final String key = timerTransactionKey(timerEntity);
                Object existing = transactionSynchronizationRegistry.getValue().getResource(key);
                //check is there is already a persist sync for this timer
                if (existing == null) {
                    transactionSynchronizationRegistry.getValue().registerInterposedSynchronization(new PersistTransactionSynchronization(lock, key));
                }
                //update the most recent version of the timer to be persisted
                transactionSynchronizationRegistry.getValue().putResource(key, timerEntity);
            }
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }

    private String timerTransactionKey(final TimerEntity timerEntity) {
        return "org.jboss.as.ejb3.timerTransactionKey." + timerEntity.getId();
    }

    @Override
    public void timerUndeployed(final String timedObjectId) {
        final Lock lock = getLock(timedObjectId);
        try {
            lock.lock();
            locks.remove(timedObjectId);
            timers.remove(timedObjectId);
            directories.remove(timedObjectId);
        } finally {
            lock.unlock();
        }

    }

    private boolean isBeforeCompletion() {
        final CurrentSynchronizationCallback.CallbackType type = CurrentSynchronizationCallback.get();
        if (type != null) {
            return type == CurrentSynchronizationCallback.CallbackType.BEFORE_COMPLETION;
        }
        return false;
    }

    @Override
    public TimerEntity loadTimer(final String id, final String timedObjectId) {
        final Lock lock = getLock(timedObjectId);
        try {
            lock.lock();
            final Map<String, TimerEntity> timers = getTimers(timedObjectId);
            final TimerEntity timer = timers.get(id);
            if(timer == null) {
                return null;
            }
            return mostRecentEntityVersion(timer);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<TimerEntity> loadActiveTimers(final String timedObjectId, Object primaryKey) {
        final Lock lock = getLock(timedObjectId);
        try {
            lock.lock();
            final Map<String, TimerEntity> timers = getTimers(timedObjectId);

            final List<TimerEntity> entities = new ArrayList<TimerEntity>();
            for(Map.Entry<String, TimerEntity> entry : timers.entrySet()) {
                if(primaryKey == null || primaryKey.equals(entry.getValue().getPrimaryKey())) {
                    entities.add(mostRecentEntityVersion(entry.getValue()));
                }
            }
            return entities;
        } finally {
            lock.unlock();
        }
    }


    @Override
    public List<TimerEntity> loadActiveTimers(final String timedObjectId) {
        return loadActiveTimers(timedObjectId, null);
    }

    /**
     * Returns either the loaded entity or the most recent version of the entity that has
     * been persisted in this transaction.
     */
    private TimerEntity mostRecentEntityVersion(final TimerEntity timerEntity) {
        try {
            final int status = transactionManager.getValue().getStatus();
            if (status == Status.STATUS_UNKNOWN ||
                    status == Status.STATUS_NO_TRANSACTION) {
                return timerEntity;
            }
            final String key = timerTransactionKey(timerEntity);
            TimerEntity existing = (TimerEntity) transactionSynchronizationRegistry.getValue().getResource(key);
            return existing != null ? existing : timerEntity;
        } catch (SystemException e) {
            throw new RuntimeException(e);
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
                ROOT_LOGGER.failToRestoreTimers(file);
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
                    ROOT_LOGGER.failToRestoreTimersFromFile(timerFile, e);
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            ROOT_LOGGER.failToCloseFile(e);
                        }
                    }
                }

            }
        } catch (Exception e) {
            ROOT_LOGGER.failToRestoreTimersForObjectId(timedObjectId, e);
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
            final File baseDir = new File(this.baseDir.getValue());
            dirName = baseDir.getAbsolutePath() + File.separator + timedObjectId.replace(File.separator, "-");
            File file = new File(dirName);
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    ROOT_LOGGER.failToCreateDirectoryForPersistTimers(file);
                }
            }
            directories.put(timedObjectId, dirName);
        }
        return dirName;
    }


    private void writeFile(TimerEntity entity) {
        final File file = fileName(entity.getTimedObjectId(), entity.getId());

        //if the timer is expired or cancelled delete the file
        if (entity.getTimerState() == TimerState.CANCELED ||
                entity.getTimerState() == TimerState.EXPIRED) {
            if (file.exists()) {
                file.delete();
            }
            return;
        }

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
                    ROOT_LOGGER.failToCloseFile(e);
                }
            }
        }
    }

    private final class PersistTransactionSynchronization implements Synchronization {

        private final String transactionKey;
        private final Lock lock;
        private volatile TimerEntity timer;

        public PersistTransactionSynchronization(final Lock lock, final String transactionKey) {
            this.lock = lock;
            this.transactionKey = transactionKey;
        }

        @Override
        public void beforeCompletion() {
            //get the latest version of the entity
            timer = (TimerEntity) transactionSynchronizationRegistry.getValue().getResource(transactionKey);
            if (timer == null) {
                return;
            }
        }

        @Override
        public void afterCompletion(final int status) {
            if (timer == null) {
                return;
            }
            try {
                lock.lock();
                if (status == Status.STATUS_COMMITTED) {
                    final Map<String, TimerEntity> map = getTimers(timer.getTimedObjectId());
                    if (timer.getTimerState() == TimerState.CANCELED ||
                            timer.getTimerState() == TimerState.EXPIRED) {
                        map.remove(timer.getId());
                    } else {
                        map.put(timer.getId(), timer);
                    }
                    writeFile(timer);
                }
            } finally {
                lock.unlock();
            }
        }


    }

    public InjectedValue<TransactionManager> getTransactionManager() {
        return transactionManager;
    }

    public InjectedValue<TransactionSynchronizationRegistry> getTransactionSynchronizationRegistry() {
        return transactionSynchronizationRegistry;
    }

    public InjectedValue<ModuleLoader> getModuleLoader() {
        return moduleLoader;
    }

    public InjectedValue<String> getBaseDir() {
        return baseDir;
    }
}
