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
package org.wildfly.iiop.openjdk.rmi;


import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.wildfly.iiop.openjdk.logging.IIOPLogger;


/**
 * Instances of this class cache the most complex analyse types.
 * <p/>
 * The analyse types cached are:
 * <ul>
 * <li><code>InterfaceAnalysis</code> for interfaces.</li>
 * <li><code>ValueAnalysis</code> for value types.</li>
 * <li><code>ExceptionAnalysis</code> for exceptions.</li>
 * </ul>
 * <p/>
 * Besides caching work already done, this caches work in progress,
 * as we need to know about this to handle cyclic graphs of analyses.
 * When a thread re-enters the <code>getAnalysis()</code> method, an
 * unfinished analysis will be returned if the same thread is already
 * working on this analysis.
 *
 * @author <a href="mailto:osh@sparre.dk">Ole Husgaard</a>
 */
class WorkCacheManager {

    /**
     * The analysis constructor of our analysis class.
     * This constructor takes a single argument of type <code>Class</code>.
     */
    private final Constructor constructor;

    /**
     * The analysis initializer of our analysis class.
     * This method takes no arguments, and is named doAnalyze.
     */
    private final Method initializer;

    /**
     * This maps the classes of completely done analyses to soft
     * references of their analysis.
     */
    private final Map<Class, SoftReference<ContainerAnalysis>> workDone;

    /**
     * This maps the classes of analyses in progress to their
     * analysis.
     */
    private final Map<InProgressKey, ContainerAnalysis> workInProgress;

    private final Map<ClassLoader, Set<Class<?>>> classesByLoader;

    /**
     * Create a new work cache manager.
     *
     * @param cls The class of the analysis type we cache here.
     */
    WorkCacheManager(final Class cls) {
        // Find the constructor and initializer.
        try {
            constructor = cls.getDeclaredConstructor(new Class[]{Class.class});
            initializer = cls.getDeclaredMethod("doAnalyze");
        } catch (NoSuchMethodException ex) {
            throw IIOPLogger.ROOT_LOGGER.unexpectedException(ex);
        }
        workDone = new HashMap<Class, SoftReference<ContainerAnalysis>>();
        workInProgress = new HashMap<InProgressKey, ContainerAnalysis>();
        classesByLoader = new HashMap<ClassLoader, Set<Class<?>>>();
    }

    public void clearClassLoader(final ClassLoader cl) {
        Set<Class<?>> classes = classesByLoader.remove(cl);
        if(classes != null) {
            for(Class<?> clazz : classes) {
                workDone.remove(clazz);
            }
        }
    }

    /**
     * Returns an analysis.
     * If the calling thread is currently doing an analysis of this
     * class, an unfinished analysis is returned.
     */
    ContainerAnalysis getAnalysis(final Class cls) throws RMIIIOPViolationException {
        ContainerAnalysis ret = null;
        boolean created = false;
        try {
            synchronized (this) {
                ret = lookupDone(cls);
                if (ret != null) {
                    return ret;
                }

                // is it work-in-progress?
                final ContainerAnalysis inProgress = workInProgress.get(new InProgressKey(cls, Thread.currentThread()));
                if (inProgress != null) {
                        return inProgress; // return unfinished

                    // Do not wait for the other thread: We may deadlock
                    // Double work is better that deadlock...
                }

                ret = createWorkInProgress(cls);
            }
            created = true;
            // Do the work
            doTheWork(cls, ret);
        } finally {
            // We did it
            synchronized (this) {
                if(created) {
                    workInProgress.remove(new InProgressKey(cls, Thread.currentThread()));
                    workDone.put(cls, new SoftReference<ContainerAnalysis>(ret));
                    ClassLoader classLoader = cls.getClassLoader();
                    if (classLoader != null) {
                        Set<Class<?>> classes = classesByLoader.get(classLoader);
                        if (classes == null) {
                            classesByLoader.put(classLoader, classes = new HashSet<Class<?>>());
                        }
                        classes.add(cls);
                    }
                }
                notifyAll();
            }
        }
        return ret;
    }

    /**
     * Lookup an analysis in the fully done map.
     */
    private ContainerAnalysis lookupDone(Class cls) {
        SoftReference ref = (SoftReference) workDone.get(cls);
        if (ref == null)
            return null;
        ContainerAnalysis ret = (ContainerAnalysis) ref.get();
        if (ret == null)
            workDone.remove(cls); // clear map entry if soft ref. was cleared.
        return ret;
    }

    /**
     * Create new work-in-progress.
     */
    private ContainerAnalysis createWorkInProgress(final Class cls) {
        final ContainerAnalysis analysis;
        try {
            analysis = (ContainerAnalysis) constructor.newInstance(cls);
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex.toString());
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex.toString());
        } catch (InvocationTargetException ex) {
            throw new RuntimeException(ex.toString());
        }

        workInProgress.put(new InProgressKey(cls, Thread.currentThread()), analysis);

        return analysis;
    }

    private void doTheWork(final Class cls, final ContainerAnalysis ret)
            throws RMIIIOPViolationException {
        try {
            initializer.invoke(ret);
        } catch (Throwable t) {
            synchronized (this) {
                workInProgress.remove(new InProgressKey(cls, Thread.currentThread()));
            }
            if (t instanceof InvocationTargetException) // unwrap
                t = ((InvocationTargetException) t).getTargetException();

            if (t instanceof RMIIIOPViolationException)
                throw (RMIIIOPViolationException) t;
            if (t instanceof RuntimeException)
                throw (RuntimeException) t;
            if (t instanceof Error)
                throw (Error) t;
            throw new RuntimeException(t.toString());
        }
    }

    private static class InProgressKey {
        final Class<?> clazz;
        final Thread thread;

        private InProgressKey(Class<?> clazz, Thread thread) {
            this.clazz = clazz;
            this.thread = thread;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            InProgressKey that = (InProgressKey) o;

            if (clazz != null ? !clazz.equals(that.clazz) : that.clazz != null) return false;
            if (thread != null ? !thread.equals(that.thread) : that.thread != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = clazz != null ? clazz.hashCode() : 0;
            result = 31 * result + (thread != null ? thread.hashCode() : 0);
            return result;
        }
    }
}

