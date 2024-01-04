/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.timerservice.persistence.filestore;

import static java.security.AccessController.doPrivileged;
import static org.jboss.as.ejb3.logging.EjbLogger.EJB3_TIMER_LOGGER;
import static org.jboss.as.ejb3.timerservice.TimerServiceImpl.safeClose;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilePermission;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.SystemException;
import jakarta.transaction.TransactionSynchronizationRegistry;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.ejb3.component.stateful.CurrentSynchronizationCallback;
import org.jboss.as.ejb3.timerservice.TimerImpl;
import org.jboss.as.ejb3.timerservice.TimerServiceImpl;
import org.jboss.as.ejb3.timerservice.TimerState;
import org.jboss.as.ejb3.timerservice.persistence.TimerPersistence;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.ModularClassResolver;
import org.jboss.marshalling.river.RiverMarshallerFactory;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jboss.staxmapper.XMLMapper;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.transaction.client.ContextTransactionManager;

/**
 * File based persistent timer store.
 * <p/>
 * TODO: this is fairly hackey at the moment, it should be registered as an XA resource to support proper XA semantics
 *
 * @author Stuart Douglas
 */
public class FileTimerPersistence implements TimerPersistence, Service {

    private static final FilePermission FILE_PERMISSION = new FilePermission("<<ALL FILES>>", "read,write,delete");
    private static final XMLInputFactory INPUT_FACTORY = XMLInputFactory.newInstance();

    private final boolean createIfNotExists;
    private MarshallerFactory factory;
    private MarshallingConfiguration configuration;
    private final Consumer<FileTimerPersistence> consumer;
    private final Supplier<TransactionSynchronizationRegistry> txnRegistrySupplier;
    private final Supplier<ModuleLoader> moduleLoaderSupplier;
    private final Supplier<PathManager> pathManagerSupplier;
    private final String path;
    private final String pathRelativeTo;
    private File baseDir;
    private PathManager.Callback.Handle callbackHandle;

    private final ConcurrentMap<String, Lock> locks = new ConcurrentHashMap<String, Lock>();
    private final ConcurrentMap<String, String> directories = new ConcurrentHashMap<String, String>();

    public FileTimerPersistence(final Consumer<FileTimerPersistence> consumer,
                                final Supplier<TransactionSynchronizationRegistry> txnRegistrySupplier,
                                final Supplier<ModuleLoader> moduleLoaderSupplier,
                                final Supplier<PathManager> pathManagerSupplier,
                                final boolean createIfNotExists, final String path, final String pathRelativeTo) {
        this.consumer = consumer;
        this.txnRegistrySupplier = txnRegistrySupplier;
        this.moduleLoaderSupplier = moduleLoaderSupplier;
        this.pathManagerSupplier = pathManagerSupplier;
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(FILE_PERMISSION);
        }
        this.createIfNotExists = createIfNotExists;
        this.path = path;
        this.pathRelativeTo = pathRelativeTo;
    }

    @Override
    public void start(final StartContext context) {
        consumer.accept(this);
        if (WildFlySecurityManager.isChecking()) {
            WildFlySecurityManager.doUnchecked(new PrivilegedAction<Void>() {
                public Void run() {
                    doStart();
                    return null;
                }
            });
        } else {
            doStart();
        }
    }

    private void doStart() {
        final RiverMarshallerFactory factory = new RiverMarshallerFactory();
        final MarshallingConfiguration configuration = new MarshallingConfiguration();
        configuration.setClassResolver(ModularClassResolver.getInstance(moduleLoaderSupplier.get()));
        configuration.setVersion(3);

        this.configuration = configuration;
        this.factory = factory;
        if (pathRelativeTo != null) {
            callbackHandle = pathManagerSupplier.get().registerCallback(pathRelativeTo, PathManager.ReloadServerCallback.create(), PathManager.Event.UPDATED, PathManager.Event.REMOVED);
        }
        baseDir = new File(pathManagerSupplier.get().resolveRelativePathEntry(path, pathRelativeTo));
        if (!baseDir.exists()) {
            if (createIfNotExists) {
                if (!baseDir.mkdirs()) {
                    throw EJB3_TIMER_LOGGER.failToCreateTimerFileStoreDir(baseDir);
                }
            } else {
                throw EJB3_TIMER_LOGGER.timerFileStoreDirNotExist(baseDir);
            }
        }
        if (!baseDir.isDirectory()) {
            throw EJB3_TIMER_LOGGER.invalidTimerFileStoreDir(baseDir);
        }
    }

    @Override
    public void stop(final StopContext context) {
        consumer.accept(null);
        locks.clear();
        directories.clear();
        if (callbackHandle != null) {
            callbackHandle.remove();
        }
        factory = null;
        configuration = null;
    }

    @Override
    public void addTimer(final TimerImpl TimerImpl) {
        if(WildFlySecurityManager.isChecking()) {
            WildFlySecurityManager.doUnchecked(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    persistTimer(TimerImpl, true);
                    return null;
                }
            });
        } else {
            persistTimer(TimerImpl, true);
        }
    }

    @Override
    public void persistTimer(final TimerImpl TimerImpl) {
        if(WildFlySecurityManager.isChecking()) {
            WildFlySecurityManager.doUnchecked(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    persistTimer(TimerImpl, false);
                    return null;
                }
            });
        } else {
            persistTimer(TimerImpl, false);
        }
    }

    @Override
    public boolean shouldRun(TimerImpl timer) {
        return true;
    }

    private void persistTimer(final TimerImpl timer, boolean newTimer) {
        final Lock lock = getLock(timer.getTimedObjectId());
        try {
            final int status = ContextTransactionManager.getInstance().getStatus();
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
                Object existing = txnRegistrySupplier.get().getResource(key);
                //check is there is already a persist sync for this timer
                if (existing == null) {
                    txnRegistrySupplier.get().registerInterposedSynchronization(new PersistTransactionSynchronization(lock, key, newTimer));
                }
                //update the most recent version of the timer to be persisted
                txnRegistrySupplier.get().putResource(key, timer);
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

    @Override
    public Closeable registerChangeListener(String timedObjectId, TimerChangeListener listener) {
        return new Closeable() {
            @Override
            public void close() throws IOException {
            }
        };
    }

    /**
     * Returns either the loaded entity or the most recent version of the entity that has
     * been persisted in this transaction.
     */
    private TimerImpl mostRecentEntityVersion(final TimerImpl timerImpl) {
        try {
            final int status = ContextTransactionManager.getInstance().getStatus();
            if (status == Status.STATUS_UNKNOWN ||
                    status == Status.STATUS_NO_TRANSACTION) {
                return timerImpl;
            }
            final String key = timerTransactionKey(timerImpl);
            TimerImpl existing = (TimerImpl) txnRegistrySupplier.get().getResource(key);
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

    private Map<String, TimerImpl> loadTimersFromFile(String timedObjectId, TimerServiceImpl timerService) {
        Map<String, TimerImpl> timers = new HashMap<>();
        String directory = getDirectory(timedObjectId);

        timers.putAll(LegacyFileStore.loadTimersFromFile(timedObjectId, timerService, directory, factory, configuration));
        for(Map.Entry<String, TimerImpl> entry : timers.entrySet()) {
            writeFile(entry.getValue()); //write legacy timers into the new format
            //the legacy code handling code will write a marker file, to make sure that the old timers will not be loaded on next restart.
        }
        final File file = new File(directory);
        if (!file.exists()) {
            //no timers exist yet
            return timers;
        } else if (!file.isDirectory()) {
            EJB3_TIMER_LOGGER.failToRestoreTimers(file);
            return timers;
        }

        final XMLMapper mapper = createMapper(timerService);

        for (File timerFile : file.listFiles()) {
            if (!timerFile.getName().endsWith(".xml")) {
                continue;
            }
            FileInputStream in = null;

            try {
                in = new FileInputStream(timerFile);
                final XMLInputFactory inputFactory = INPUT_FACTORY;
                setIfSupported(inputFactory, XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
                setIfSupported(inputFactory, XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
                final XMLStreamReader streamReader = inputFactory.createXMLStreamReader(in);
                try {
                    List<TimerImpl> timerList = new ArrayList<>();
                    mapper.parseDocument(timerList, streamReader);
                    for (TimerImpl timer : timerList) {
                        if (timer.getId().equals("deleted-timer")) {
                            timerFile.delete();
                            break;
                        }
                        timers.put(timer.getId(), timer);
                    }
                } finally {
                    safeClose(in);
                }

            } catch (Exception e) {
                EJB3_TIMER_LOGGER.failToRestoreTimersFromFile(timerFile, e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        EJB3_TIMER_LOGGER.failToCloseFile(e);
                    }
                }
            }
        }
        return timers;
    }

    private XMLMapper createMapper(TimerServiceImpl timerService) {
        final XMLMapper mapper = XMLMapper.Factory.create();
        mapper.registerRootElement(new QName(EjbTimerXmlParser_1_0.NAMESPACE, EjbTimerXmlPersister.TIMERS), new EjbTimerXmlParser_1_0(timerService, factory, configuration, timerService.getInvoker().getClassLoader()));
        return mapper;
    }


    private File fileName(String timedObjectId, String timerId) {
        return new File(getDirectory(timedObjectId) + File.separator + timerId.replace(File.separator, "-") + ".xml");
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
            if (!file.exists() && !file.mkdirs()) {
                EJB3_TIMER_LOGGER.failToCreateDirectoryForPersistTimers(file);
            }
            directories.put(timedObjectId, dirName);
        }
        return dirName;
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
            timer = (TimerImpl) txnRegistrySupplier.get().getResource(transactionKey);
            if (timer == null) {
                return;
            }
        }

        @Override
        public void afterCompletion(final int status) {
            doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    if (timer == null) {
                        return null;
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
                    return null;
                }
            });
        }


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
        try {
            FileOutputStream out = new FileOutputStream(file);

            try {
                XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(out);
                XMLMapper mapper = createMapper(timer.getTimerService());
                mapper.deparseDocument(new EjbTimerXmlPersister(factory, configuration), Collections.singletonList(timer), writer);
                writer.flush();
                writer.close();
            } finally {
                safeClose(out);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setIfSupported(final XMLInputFactory inputFactory, final String property, final Object value) {
        if (inputFactory.isPropertySupported(property)) {
            inputFactory.setProperty(property, value);
        }
    }

    public static XMLExtendedStreamWriter create(XMLStreamWriter writer) throws Exception {
        // Use reflection to access package protected class FormattingXMLStreamWriter
        // TODO: at some point the staxmapper API could be enhanced to make this unnecessary
        Class<?> clazz = Class.forName("org.jboss.staxmapper.FormattingXMLStreamWriter");
        Constructor<?> ctr = clazz.getConstructor(XMLStreamWriter.class);
        ctr.setAccessible(true);
        return (XMLExtendedStreamWriter) ctr.newInstance(new Object[]{writer});
    }
}
