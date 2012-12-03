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

import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.ejb3.component.stateful.CurrentSynchronizationCallback;
import org.jboss.as.ejb3.timerservice.CalendarTimer;
import org.jboss.as.ejb3.timerservice.TimerImpl;
import org.jboss.as.ejb3.timerservice.TimerServiceImpl;
import org.jboss.as.ejb3.timerservice.TimerState;
import org.jboss.as.ejb3.timerservice.persistence.CalendarTimerEntity;
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
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import static org.jboss.as.ejb3.EjbLogger.ROOT_LOGGER;
import static org.jboss.as.ejb3.EjbMessages.MESSAGES;

/**
 * File based persistent timer store.
 * <p/>
 * TODO: this is fairly hackey at the moment, it should be registered as an XA resource to support proper XA semantics
 *
 * @author Stuart Douglas
 */
public class FileTimerPersistence implements TimerPersistence, Service<FileTimerPersistence> {

    private final boolean createIfNotExists;
    private MarshallerFactory factory;
    private MarshallingConfiguration configuration;
    private final InjectedValue<TransactionManager> transactionManager = new InjectedValue<TransactionManager>();
    private final InjectedValue<TransactionSynchronizationRegistry> transactionSynchronizationRegistry = new InjectedValue<TransactionSynchronizationRegistry>();
    private final InjectedValue<ModuleLoader> moduleLoader = new InjectedValue<ModuleLoader>();
    private final InjectedValue<PathManager> pathManager = new InjectedValue<PathManager>();
    private final String path;
    private final String pathRelativeTo;
    private File baseDir;
    private PathManager.Callback.Handle callbackHandle;

    private final ConcurrentMap<String, Lock> locks = new ConcurrentHashMap<String, Lock>();
    private final ConcurrentMap<String, String> directories = new ConcurrentHashMap<String, String>();

    public FileTimerPersistence(final boolean createIfNotExists, final String path, final String pathRelativeTo) {
        this.createIfNotExists = createIfNotExists;
        this.path = path;
        this.pathRelativeTo = pathRelativeTo;
    }

    @Override
    public synchronized void start(final StartContext context) {

        final RiverMarshallerFactory factory = new RiverMarshallerFactory();
        final MarshallingConfiguration configuration = new MarshallingConfiguration();
        configuration.setClassResolver(ModularClassResolver.getInstance(moduleLoader.getValue()));

        this.configuration = configuration;
        this.factory = factory;
        if (pathRelativeTo != null) {
            callbackHandle = pathManager.getValue().registerCallback(pathRelativeTo, PathManager.ReloadServerCallback.create(), PathManager.Event.UPDATED, PathManager.Event.REMOVED);
        }
        baseDir = new File(pathManager.getValue().resolveRelativePathEntry(path, pathRelativeTo));
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
    }

    @Override
    public void stop(final StopContext context) {
        locks.clear();
        directories.clear();
        if (callbackHandle != null) {
            callbackHandle.remove();
        }
        factory = null;
        configuration = null;
    }

    @Override
    public FileTimerPersistence getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void addTimer(final TimerImpl TimerImpl) {
        persistTimer(TimerImpl, true);
    }

    @Override
    public void persistTimer(final TimerImpl TimerImpl) {
        persistTimer(TimerImpl, false);
    }

    private void persistTimer(final TimerImpl timer, boolean newTimer) {
        final Lock lock = getLock(timer.getTimedObjectId());
        try {
            final int status = transactionManager.getValue().getStatus();
            if (status == Status.STATUS_MARKED_ROLLBACK || status == Status.STATUS_ROLLEDBACK ||
                    status == Status.STATUS_ROLLING_BACK) {
                //no need to persist anyway
                return;
            }

            lock.lock();
            if (status == Status.STATUS_NO_TRANSACTION ||
                    status == Status.STATUS_UNKNOWN || isBeforeCompletion()
                    || status == Status.STATUS_COMMITTED) {
                Map<String, TimerImpl> map = getTimers(timer.getTimedObjectId(), timer.getTimerService());
                if (timer.getState() == TimerState.CANCELED ||
                        timer.getState() == TimerState.EXPIRED) {
                    map.remove(timer.getId());
                    writeFile(timer);
                } else if (newTimer || map.containsKey(timer.getId())) {
                    //if it is not a new timer and is not in the map then it has
                    //been removed by another thread.
                    map.put(timer.getId(), timer);
                    writeFile(timer);
                }
            } else {

                final String key = timerTransactionKey(timer);
                Object existing = transactionSynchronizationRegistry.getValue().getResource(key);
                //check is there is already a persist sync for this timer
                if (existing == null) {
                    transactionSynchronizationRegistry.getValue().registerInterposedSynchronization(new PersistTransactionSynchronization(lock, key, newTimer));
                }
                //update the most recent version of the timer to be persisted
                transactionSynchronizationRegistry.getValue().putResource(key, timer);
            }
        } catch (SystemException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    private String timerTransactionKey(final TimerImpl TimerImpl) {
        return "org.jboss.as.ejb3.timerTransactionKey." + TimerImpl.getId();
    }

    @Override
    public void timerUndeployed(final String timedObjectId) {
        final Lock lock = getLock(timedObjectId);
        try {
            lock.lock();
            locks.remove(timedObjectId);
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
    public List<TimerImpl> loadActiveTimers(final String timedObjectId, final TimerServiceImpl timerService) {
        final Lock lock = getLock(timedObjectId);
        try {
            lock.lock();
            final Map<String, TimerImpl> timers = getTimers(timedObjectId, timerService);

            final List<TimerImpl> entities = new ArrayList<TimerImpl>();
            for (Map.Entry<String, TimerImpl> entry : timers.entrySet()) {
                entities.add(mostRecentEntityVersion(entry.getValue()));
            }
            return entities;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Returns either the loaded entity or the most recent version of the entity that has
     * been persisted in this transaction.
     */
    private TimerImpl mostRecentEntityVersion(final TimerImpl timerImpl) {
        try {
            final int status = transactionManager.getValue().getStatus();
            if (status == Status.STATUS_UNKNOWN ||
                    status == Status.STATUS_NO_TRANSACTION) {
                return timerImpl;
            }
            final String key = timerTransactionKey(timerImpl);
            TimerImpl existing = (TimerImpl) transactionSynchronizationRegistry.getValue().getResource(key);
            return existing != null ? existing : timerImpl;
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
    private Map<String, TimerImpl> getTimers(final String timedObjectId, final TimerServiceImpl timerService) {
        return loadTimersFromFile(timedObjectId, timerService);
    }

    private Map<String, TimerImpl> loadTimersFromFile(final String timedObjectId, final TimerServiceImpl timerService) {
        final Map<String, TimerImpl> timers = new HashMap<String, TimerImpl>();
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

                    //we load the legacy timer entity class, and turn it into a timer state

                    TimerImpl.Builder builder;
                    if (entity instanceof CalendarTimerEntity) {
                        CalendarTimerEntity c = (CalendarTimerEntity) entity;
                        builder = CalendarTimer.builder()
                                .setScheduleExprSecond(c.getSecond())
                                .setScheduleExprMinute(c.getMinute())
                                .setScheduleExprHour(c.getHour())
                                .setScheduleExprDayOfWeek(c.getDayOfWeek())
                                .setScheduleExprDayOfMonth(c.getDayOfMonth())
                                .setScheduleExprMonth(c.getMonth())
                                .setScheduleExprYear(c.getYear())
                                .setScheduleExprStartDate(c.getStartDate())
                                .setScheduleExprEndDate(c.getEndDate())
                                .setScheduleExprTimezone(c.getTimezone())
                                .setAutoTimer(c.isAutoTimer())
                                .setTimeoutMethod(CalendarTimer.getTimeoutMethod(c.getTimeoutMethod(), timerService.getTimedObjectInvoker().getValue()));
                    } else {
                        builder = TimerImpl.builder();
                    }
                    builder.setId(entity.getId())
                            .setTimedObjectId(entity.getTimedObjectId())
                            .setInitialDate(entity.getInitialDate())
                            .setRepeatInterval(entity.getInterval())
                            .setNextDate(entity.getNextDate())
                            .setPreviousRun(entity.getPreviousRun())
                            .setInfo(entity.getInfo())
                            .setPrimaryKey(entity.getPreviousRun())
                            .setTimerState(entity.getTimerState());

                    timers.put(entity.getId(), builder.build(timerService));
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


    private void writeFile(TimerImpl timer) {
        final File file = fileName(timer.getTimedObjectId(), timer.getId());

        //if the timer is expired or cancelled delete the file
        if (timer.getState() == TimerState.CANCELED ||
                timer.getState() == TimerState.EXPIRED) {
            if (file.exists()) {
                file.delete();
            }
            return;
        }

        final TimerEntity entity;
        if (timer instanceof CalendarTimer) {
            entity = new CalendarTimerEntity((CalendarTimer) timer);
        } else {
            entity = new TimerEntity(timer);
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
        private final boolean newTimer;
        private volatile TimerImpl timer;

        public PersistTransactionSynchronization(final Lock lock, final String transactionKey, final boolean newTimer) {
            this.lock = lock;
            this.transactionKey = transactionKey;
            this.newTimer = newTimer;
        }

        @Override
        public void beforeCompletion() {
            //get the latest version of the entity
            timer = (TimerImpl) transactionSynchronizationRegistry.getValue().getResource(transactionKey);
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
                    final Map<String, TimerImpl> map = getTimers(timer.getTimedObjectId(), timer.getTimerService());
                    if (timer.getState() == TimerState.CANCELED ||
                            timer.getState() == TimerState.EXPIRED) {
                        map.remove(timer.getId());
                    } else {
                        if (newTimer || map.containsKey(timer.getId())) {
                            //if an existing timer is not in the map it has been cancelled by another thread
                            map.put(timer.getId(), timer);
                        }
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

    public InjectedValue<PathManager> getPathManager() {
        return pathManager;
    }


}
