/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jmx;

import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.OperationsException;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.loading.ClassLoaderRepository;

import org.jboss.as.controller.audit.AuditLogger;
import org.jboss.as.server.jmx.MBeanServerPlugin;
import org.jboss.as.server.jmx.PluggableMBeanServer;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * An MBeanServer supporting {@link MBeanServerPlugin}s. At it's core is the original platform mbean server wrapped in TCCL behaviour.
 * <em>Note:</em> If this class name changes ModelCombiner must be updated.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class PluggableMBeanServerImpl implements PluggableMBeanServer {

    private final MBeanServerPlugin rootMBeanServer;
    private volatile AuditLoggerInfo auditLoggerInfo = NOOP_INFO;

    private final Set<MBeanServerPlugin> delegates = new CopyOnWriteArraySet<MBeanServerPlugin>();

    PluggableMBeanServerImpl(MBeanServer rootMBeanServer) {
        this.rootMBeanServer = new TcclMBeanServer(rootMBeanServer);
    }

    void setAuditLogger(AuditLoggerInfo auditLoggerInfo) {
        this.auditLoggerInfo = auditLoggerInfo != null ? auditLoggerInfo : NOOP_INFO;
    }

    AuditLogger getAuditLogger() {
        return auditLoggerInfo.getAuditLogger();
    }

    boolean isBooting() {
        return auditLoggerInfo.isBooting();
    }

    boolean shouldLog(boolean readOnly) {
        return auditLoggerInfo.shouldLog(readOnly);
    }

    public void addPlugin(MBeanServerPlugin delegate) {
        delegates.add(delegate);
    }

    public void removePlugin(MBeanServerPlugin delegate) {
        delegates.remove(delegate);
    }

    @Override
    public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback)
            throws InstanceNotFoundException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        try {
            delegate = findDelegate(name);
            delegate.addNotificationListener(name, listener, filter, handback);
        } catch (Exception e) {
            error = e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            if (e instanceof RuntimeException) throw (RuntimeException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).addNotificationListener(name, listener, filter, handback);
            }
        }
    }

    @Override
    public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback)
            throws InstanceNotFoundException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        try {
            delegate = findDelegate(name);
            delegate.addNotificationListener(name, listener, filter, handback);
        } catch (Exception e) {
            error = e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            if (e instanceof RuntimeException) throw (RuntimeException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).addNotificationListener(name, listener, filter, handback);
            }
        }
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanException,
            NotCompliantMBeanException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        try {
            delegate = findDelegateForNewObject(name);
            return delegate.createMBean(className, name, params, signature);
        } catch (Exception e) {
            error = e;
            if (e instanceof ReflectionException) throw (ReflectionException)e;
            if (e instanceof InstanceAlreadyExistsException) throw (InstanceAlreadyExistsException)e;
            if (e instanceof MBeanException) throw (MBeanException)e;
            if (e instanceof NotCompliantMBeanException) throw (NotCompliantMBeanException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).createMBean(className, name, params, signature);
            }
        }
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params,
            String[] signature) throws ReflectionException, InstanceAlreadyExistsException,
            MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        try {
            delegate = findDelegate(name);
            return delegate.createMBean(className, name, loaderName, params, signature);
        } catch (Exception e) {
            error = e;
            if (e instanceof ReflectionException) throw (ReflectionException)e;
            if (e instanceof InstanceAlreadyExistsException) throw (InstanceAlreadyExistsException)e;
            if (e instanceof MBeanException) throw (MBeanException)e;
            if (e instanceof NotCompliantMBeanException) throw (NotCompliantMBeanException)e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).createMBean(className, name, params, signature);
            }
        }
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName) throws ReflectionException,
            InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException,
            InstanceNotFoundException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        try {
            delegate = findDelegate(name);
            return delegate.createMBean(className, name, loaderName);
        } catch (Exception e) {
            error = e;
            if (e instanceof ReflectionException) throw (ReflectionException)e;
            if (e instanceof InstanceAlreadyExistsException) throw (InstanceAlreadyExistsException)e;
            if (e instanceof MBeanException) throw (MBeanException)e;
            if (e instanceof NotCompliantMBeanException) throw (NotCompliantMBeanException)e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).createMBean(className, name, loaderName);
            }
        }
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name) throws ReflectionException,
            InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        try {
            delegate = findDelegateForNewObject(name);
            return delegate.createMBean(className, name);
        } catch (Exception e) {
            error = e;
            if (e instanceof ReflectionException) throw (ReflectionException)e;
            if (e instanceof InstanceAlreadyExistsException) throw (InstanceAlreadyExistsException)e;
            if (e instanceof MBeanException) throw (MBeanException)e;
            if (e instanceof NotCompliantMBeanException) throw (NotCompliantMBeanException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).createMBean(className, name);
            }
        }
    }

    @Override
    @Deprecated
    public ObjectInputStream deserialize(ObjectName name, byte[] data) throws OperationsException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        try {
            delegate = findDelegate(name);
            return delegate.deserialize(name, data);
        } catch (Exception e) {
            error = e;
            if (e instanceof OperationsException) throw (OperationsException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).deserialize(name, data);
            }
        }
    }

    @Override
    @Deprecated
    public ObjectInputStream deserialize(String className, byte[] data) throws OperationsException, ReflectionException {
        Throwable error = null;
        MBeanServerPlugin delegate = rootMBeanServer;
        try {
            return delegate.deserialize(className, data);
        } catch (Exception e) {
            error = e;
            if (e instanceof OperationsException) throw (OperationsException)e;
            if (e instanceof ReflectionException) throw (ReflectionException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).deserialize(className, data);
            }
        }
    }

    @Override
    @Deprecated
    public ObjectInputStream deserialize(String className, ObjectName loaderName, byte[] data) throws OperationsException, ReflectionException {
        Throwable error = null;
        MBeanServerPlugin delegate = rootMBeanServer;
        try {
            return delegate.deserialize(className, loaderName, data);
        } catch (Exception e) {
            error = e;
            if (e instanceof OperationsException) throw (OperationsException)e;
            if (e instanceof ReflectionException) throw (ReflectionException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).deserialize(className, loaderName, data);
            }
        }
    }

    @Override
    public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException,
            InstanceNotFoundException, ReflectionException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        try {
            delegate = findDelegate(name);
            return delegate.getAttribute(name, attribute);
        } catch (Exception e) {
            error = e;
            if (e instanceof MBeanException) throw (MBeanException)e;
            if (e instanceof AttributeNotFoundException) throw (AttributeNotFoundException)e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            if (e instanceof ReflectionException) throw (ReflectionException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).getAttribute(name, attribute);
            }
        }
    }

    @Override
    public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException,
            ReflectionException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        try {
            delegate = findDelegate(name);
            return delegate.getAttributes(name, attributes);
        } catch (Exception e) {
            error = e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            if (e instanceof ReflectionException) throw (ReflectionException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).getAttributes(name, attributes);
            }
        }
    }

    @Override
    public ClassLoader getClassLoader(ObjectName loaderName) throws InstanceNotFoundException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        try {
            delegate = findDelegate(loaderName);
            return delegate.getClassLoader(loaderName);
        } catch (Exception e) {
            error = e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).getClassLoader(loaderName);
            }
        }
    }

    @Override
    public ClassLoader getClassLoaderFor(ObjectName mbeanName) throws InstanceNotFoundException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        try {
            delegate = findDelegate(mbeanName);
            return delegate.getClassLoaderFor(mbeanName);
        } catch (Exception e) {
            error = e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).getClassLoaderFor(mbeanName);
            }
        }
    }

    @Override
    public ClassLoaderRepository getClassLoaderRepository() {
        Throwable error = null;
        MBeanServerPlugin delegate = rootMBeanServer;
        try {
            return delegate.getClassLoaderRepository();
        } catch (Exception e) {
            error = e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).getClassLoaderRepository();
            }
        }
    }

    @Override
    public String getDefaultDomain() {
        Throwable error = null;
        MBeanServerPlugin delegate = rootMBeanServer;
        try {
            return delegate.getDefaultDomain();
        } catch (Exception e) {
            error = e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).getDefaultDomain();
            }
        }
    }

    @Override
    public String[] getDomains() {
        Throwable error = null;
        try {
            ArrayList<String> result = new ArrayList<String>();
            if (delegates.size() > 0) {
                for (MBeanServerPlugin delegate : delegates) {
                    String[] domains = delegate.getDomains();
                    if (domains.length > 0) {
                        result.addAll(Arrays.asList(domains));
                    }
                }
            }
            result.addAll(Arrays.asList(rootMBeanServer.getDomains()));
            return result.toArray(new String[result.size()]);
        } catch (Exception e) {
            error = e;
            throw makeRuntimeException(e);
        } finally {
            //This should always audit log
            new MBeanServerAuditLogger(this, error).getDomains();
        }
    }

    @Override
    public Integer getMBeanCount() {
        Throwable error = null;
        try {
            int i = 0;
            if (delegates.size() > 0) {
                for (MBeanServerPlugin delegate : delegates) {
                    i += delegate.getMBeanCount();
                }
            }
            i += rootMBeanServer.getMBeanCount();
            return i;
        } catch (Exception e) {
            error = e;
            throw makeRuntimeException(e);
        } finally {
            //always audit log this
            new MBeanServerAuditLogger(this, error).getMBeanCount();
        }
    }

    @Override
    public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException {
        return getMBeanInfo(name, true, false);
    }

    MBeanInfo getMBeanInfo(ObjectName name, boolean log, boolean nullIfNotFound) throws InstanceNotFoundException, IntrospectionException, ReflectionException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        try {
            delegate = findDelegate(name);
            return delegate.getMBeanInfo(name);
        } catch (Exception e) {
            if (nullIfNotFound) {
                return null;
            }
            error = e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            if (e instanceof IntrospectionException) throw (IntrospectionException)e;
            if (e instanceof ReflectionException) throw (ReflectionException)e;
            throw makeRuntimeException(e);
        } finally {
            if (log && shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).getMBeanInfo(name);
            }
        }
    }

    @Override
    public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        try {
            delegate = findDelegate(name);
            return delegate.getObjectInstance(name);
        } catch (Exception e) {
            error = e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).getObjectInstance(name);
            }
        }
    }

    @Override
    public Object instantiate(String className, Object[] params, String[] signature) throws ReflectionException, MBeanException {
        Throwable error = null;
        MBeanServerPlugin delegate = rootMBeanServer;
        try {
            return delegate.instantiate(className, params, signature);
        } catch (Exception e) {
            error = e;
            if (e instanceof ReflectionException) throw (ReflectionException)e;
            if (e instanceof MBeanException) throw (MBeanException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).instantiate(className, params, signature);
            }
        }
    }

    @Override
    public Object instantiate(String className, ObjectName loaderName, Object[] params, String[] signature)
            throws ReflectionException, MBeanException, InstanceNotFoundException {
        Throwable error = null;
        MBeanServerPlugin delegate = rootMBeanServer;
        try {
            return delegate.instantiate(className, loaderName, params, signature);
        } catch (Exception e) {
            error = e;
            if (e instanceof ReflectionException) throw (ReflectionException)e;
            if (e instanceof MBeanException) throw (MBeanException)e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).instantiate(className, loaderName, params, signature);
            }
        }
    }

    @Override
    public Object instantiate(String className, ObjectName loaderName) throws ReflectionException, MBeanException,
            InstanceNotFoundException {
        Throwable error = null;
        MBeanServerPlugin delegate = rootMBeanServer;
        try {
            return delegate.instantiate(className, loaderName);
        } catch (Exception e) {
            error = e;
            if (e instanceof ReflectionException) throw (ReflectionException)e;
            if (e instanceof MBeanException) throw (MBeanException)e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).instantiate(className, loaderName);
            }
        }
    }

    @Override
    public Object instantiate(String className) throws ReflectionException, MBeanException {
        Throwable error = null;
        MBeanServerPlugin delegate = rootMBeanServer;
        try {
            return delegate.instantiate(className);
        } catch (Exception e) {
            error = e;
            if (e instanceof ReflectionException) throw (ReflectionException)e;
            if (e instanceof MBeanException) throw (MBeanException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).instantiate(className);
            }
        }
    }

    @Override
    public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature)
            throws InstanceNotFoundException, MBeanException, ReflectionException {
        Throwable error = null;
        MBeanServerPlugin delegate = findDelegate(name);
        try {
            //TODO need to determine impact for authorization
            delegate = findDelegate(name);
            return delegate.invoke(name, operationName, params, signature);
        } catch (Exception e) {
            error = e;
            if (e instanceof ReflectionException) throw (ReflectionException)e;
            if (e instanceof MBeanException) throw (MBeanException)e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).invoke(name, operationName, params, signature);
            }
        }
    }

    @Override
    public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        try {
            delegate = findDelegate(name);
            return delegate.isInstanceOf(name, className);
        } catch (Exception e) {
            error = e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).isInstanceOf(name, className);
            }
        }
    }

    @Override
    public boolean isRegistered(ObjectName name) {
        Throwable error = null;
        Boolean shouldAuditLog = null;
        try {
            if (delegates.size() > 0) {
                for (MBeanServerPlugin delegate : delegates) {
                    if (delegate.accepts(name) && delegate.isRegistered(name)) {
                        if (delegate.shouldAuditLog()) {
                            shouldAuditLog = true;
                        }
                        return true;
                    }
                }
            }
            // check if it's registered with the root (a.k.a platform) MBean server
            shouldAuditLog = true;
            return rootMBeanServer.isRegistered(name);
        } catch (Exception e) {
            error = e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog == null || shouldAuditLog) {
                new MBeanServerAuditLogger(this, error).isRegistered(name);
            }
        }

    }

    @Override
    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
        Throwable error = null;
        try {
            Set<ObjectInstance> result = new HashSet<ObjectInstance>();
            if (delegates.size() > 0) {
                for (MBeanServerPlugin delegate : delegates) {
                    if (name == null || (name.getDomain() != null && delegate.accepts(name))) {
                        result.addAll(delegate.queryMBeans(name, query));
                    }
                }
            }
            result.addAll(rootMBeanServer.queryMBeans(name, query));
            return result;
        } catch (Exception e) {
            error = e;
            throw makeRuntimeException(e);
        } finally {
            //This should always audit log
            new MBeanServerAuditLogger(this, error).queryMBeans(name, query);
        }
    }

    @Override
    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
        Throwable error = null;
        try {
            Set<ObjectName> result = new HashSet<ObjectName>();
            if (delegates.size() > 0) {
                for (MBeanServerPlugin delegate : delegates) {
                    if (name == null || (name.getDomain() != null && delegate.accepts(name))) {
                        result.addAll(delegate.queryNames(name, query));
                    }
                }
            }
            result.addAll(rootMBeanServer.queryNames(name, query));
            return result;
        } catch (Exception e) {
            error = e;
            throw makeRuntimeException(e);
        } finally {
            //This should always audit log
            new MBeanServerAuditLogger(this, error).queryNames(name, query);
        }
    }

    @Override
    public ObjectInstance registerMBean(Object object, ObjectName name) throws InstanceAlreadyExistsException,
            MBeanRegistrationException, NotCompliantMBeanException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        try {
            delegate = findDelegateForNewObject(name);
            return delegate.registerMBean(object, name);
        } catch (Exception e) {
            error = e;
            if (e instanceof InstanceAlreadyExistsException) throw (InstanceAlreadyExistsException)e;
            if (e instanceof MBeanRegistrationException) throw (MBeanRegistrationException)e;
            if (e instanceof NotCompliantMBeanException) throw (NotCompliantMBeanException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).registerMBean(object, name);
            }
        }
    }

    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter,
            Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        try {
            delegate = findDelegate(name);
            delegate.removeNotificationListener(name, listener, filter, handback);
        } catch (Exception e) {
            error = e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            if (e instanceof ListenerNotFoundException) throw (ListenerNotFoundException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).removeNotificationListener(name, listener, filter, handback);
            }
        }
    }

    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException,
            ListenerNotFoundException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        try {
            delegate = findDelegate(name);
            delegate.removeNotificationListener(name, listener);
        } catch (Exception e) {
            error = e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            if (e instanceof ListenerNotFoundException) throw (ListenerNotFoundException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).removeNotificationListener(name, listener);
            }
        }
    }

    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        try {
            delegate = findDelegate(name);
            delegate.removeNotificationListener(name, listener, filter, handback);
        } catch (Exception e) {
            error = e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            if (e instanceof ListenerNotFoundException) throw (ListenerNotFoundException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).removeNotificationListener(name, listener, filter, handback);
            }
        }
    }

    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException,
            ListenerNotFoundException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        try {
            delegate = findDelegate(name);
            delegate.removeNotificationListener(name, listener);
        } catch (Exception e) {
            error = e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            if (e instanceof ListenerNotFoundException) throw (ListenerNotFoundException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).removeNotificationListener(name, listener);
            }
        }
    }

    @Override
    public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException,
            InvalidAttributeValueException, MBeanException, ReflectionException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        try {
            delegate = findDelegate(name);
            delegate.setAttribute(name, attribute);
        } catch (Exception e) {
            error = e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            if (e instanceof AttributeNotFoundException) throw (AttributeNotFoundException)e;
            if (e instanceof InvalidAttributeValueException) throw (InvalidAttributeValueException)e;
            if (e instanceof MBeanException) throw (MBeanException)e;
            if (e instanceof ReflectionException) throw (ReflectionException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).setAttribute(name, attribute);
            }
        }
    }

    @Override
    public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException,
            ReflectionException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        try {
            delegate = findDelegate(name);
            return delegate.setAttributes(name, attributes);
        } catch (Exception e) {
            error = e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            if (e instanceof ReflectionException) throw (ReflectionException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).setAttributes(name, attributes);
            }
        }
    }

    @Override
    public void unregisterMBean(ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        try {
            delegate = findDelegate(name);
            delegate.unregisterMBean(name);
        } catch (Exception e) {
            error = e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            if (e instanceof MBeanRegistrationException) throw (MBeanRegistrationException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate)) {
                new MBeanServerAuditLogger(this, error).unregisterMBean(name);
            }
        }
    }

    private MBeanServerPlugin findDelegate(ObjectName name) throws InstanceNotFoundException {
        if (name == null) {
            throw JmxMessages.MESSAGES.objectNameCantBeNull();
        }
        if (delegates.size() > 0) {
            for (MBeanServerPlugin delegate : delegates) {
                if (delegate.accepts(name) && delegate.isRegistered(name)) {
                    return delegate;
                }
            }
        }
        if (rootMBeanServer.isRegistered(name)) {
            return rootMBeanServer;
        }
        throw new InstanceNotFoundException(name.toString());
    }

    private MBeanServerPlugin findDelegateForNewObject(ObjectName name) {
        if (name == null) {
            throw JmxMessages.MESSAGES.objectNameCantBeNull();
        }
        if (delegates.size() > 0) {
            for (MBeanServerPlugin delegate : delegates) {
                if (delegate.accepts(name)) {
                    return delegate;
                }
            }
        }
        return rootMBeanServer;
    }

    private boolean shouldAuditLog(MBeanServerPlugin delegate) {
        if (delegate == null) {
            return true;
        }
        return delegate.shouldAuditLog();
    }

    private RuntimeException makeRuntimeException(Exception e) {
        if (e instanceof RuntimeException) {
            return (RuntimeException)e;
        }
        return new RuntimeException(e);
    }

    private class TcclMBeanServer implements MBeanServerPlugin {

        private final MBeanServer delegate;

        public TcclMBeanServer(MBeanServer delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean accepts(ObjectName objectName) {
            //Will never be called
            return true;
        }

        @Override
        public boolean shouldAuditLog() {
            return true;
        }

        @Override
        public boolean shouldAuthorize() {
            return true;
        }

        public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback)
                throws InstanceNotFoundException {
            ClassLoader old = pushClassLoader(name);
            try {
                delegate.addNotificationListener(name, listener, filter, handback);
            } finally {
                resetClassLoader(old);
            }

        }

        public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback)
                throws InstanceNotFoundException {
            ClassLoader old = pushClassLoader(name);
            try {
                delegate.addNotificationListener(name, listener, filter, handback);
            } finally {
                resetClassLoader(old);
            }
        }

        public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature) throws ReflectionException,
                InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException {
            return delegate.createMBean(className, name, params, signature);
        }

        public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params, String[] signature)
                throws ReflectionException, InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException,
                InstanceNotFoundException {
            ClassLoader old = pushClassLoaderByName(loaderName);
            try {
                return delegate.createMBean(className, name, loaderName, params, signature);
            } finally {
                resetClassLoader(old);
            }
        }

        public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName) throws ReflectionException,
                InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException {

            ClassLoader old = pushClassLoaderByName(loaderName);
            try {
                return delegate.createMBean(className, name, loaderName);
            } finally {
                resetClassLoader(old);
            }

        }

        public ObjectInstance createMBean(String className, ObjectName name) throws ReflectionException, InstanceAlreadyExistsException,
                 MBeanException, NotCompliantMBeanException {
            return delegate.createMBean(className, name);
        }

        @Override
        @Deprecated
        public ObjectInputStream deserialize(ObjectName name, byte[] data) throws OperationsException {
            return delegate.deserialize(name, data);
        }

        @Override
        @Deprecated
        public ObjectInputStream deserialize(String className, byte[] data) throws OperationsException, ReflectionException {
            return delegate.deserialize(className, data);
        }

        @Override
        @Deprecated
        public ObjectInputStream deserialize(String className, ObjectName loaderName, byte[] data) throws OperationsException,
                ReflectionException {
            return delegate.deserialize(className, loaderName, data);
        }

        public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException,
                ReflectionException {
            ClassLoader old = pushClassLoader(name);
            try {
                return delegate.getAttribute(name, attribute);
            } finally {
                resetClassLoader(old);
            }
        }

        public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException {
            ClassLoader old = pushClassLoader(name);
            try {
                return delegate.getAttributes(name, attributes);
            } finally {
                resetClassLoader(old);
            }
        }

        public ClassLoader getClassLoader(ObjectName loaderName) throws InstanceNotFoundException {
            return delegate.getClassLoader(loaderName);
        }

        public ClassLoader getClassLoaderFor(ObjectName mbeanName) throws InstanceNotFoundException {
            return delegate.getClassLoaderFor(mbeanName);
        }

        public ClassLoaderRepository getClassLoaderRepository() {
            return delegate.getClassLoaderRepository();
        }

        public String getDefaultDomain() {
            return delegate.getDefaultDomain();
        }

        public String[] getDomains() {
            return delegate.getDomains();
        }

        public Integer getMBeanCount() {
            return delegate.getMBeanCount();
        }

        public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException {
            return delegate.getMBeanInfo(name);
        }

        public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException {
            return delegate.getObjectInstance(name);
        }

        public Object instantiate(String className, Object[] params, String[] signature) throws ReflectionException, MBeanException {
            return delegate.instantiate(className, params, signature);
        }

        public Object instantiate(String className, ObjectName loaderName, Object[] params, String[] signature) throws ReflectionException,
                MBeanException, InstanceNotFoundException {
            ClassLoader old = pushClassLoaderByName(loaderName);
            try {
                return delegate.instantiate(className, loaderName, params, signature);
            } finally {
                resetClassLoader(old);
            }
        }

        public Object instantiate(String className, ObjectName loaderName) throws ReflectionException, MBeanException, InstanceNotFoundException {
            ClassLoader old = pushClassLoaderByName(loaderName);
            try {
                return delegate.instantiate(className, loaderName);
            } finally {
                resetClassLoader(old);
            }
        }

        public Object instantiate(String className) throws ReflectionException, MBeanException {
            return delegate.instantiate(className);
        }

        public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) throws InstanceNotFoundException,
                MBeanException, ReflectionException {

            ClassLoader old = pushClassLoader(name);
            try {
                return delegate.invoke(name, operationName, params, signature);
            } finally {
                resetClassLoader(old);
            }
        }

        public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException {
            return delegate.isInstanceOf(name, className);
        }

        public boolean isRegistered(ObjectName name) {
            return delegate.isRegistered(name);
        }

        public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
            return delegate.queryMBeans(name, query);
        }

        public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
            return delegate.queryNames(name, query);
        }

        public ObjectInstance registerMBean(Object object, ObjectName name) throws InstanceAlreadyExistsException, MBeanRegistrationException,
                NotCompliantMBeanException {
            return delegate.registerMBean(object, name);
        }

        public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback)
                throws InstanceNotFoundException, ListenerNotFoundException {
            ClassLoader old = pushClassLoader(name);
            try {
                delegate.removeNotificationListener(name, listener, filter, handback);
            } finally {
                resetClassLoader(old);
            }
        }

        public void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException,
                ListenerNotFoundException {
            ClassLoader old = pushClassLoader(name);
            try {
                delegate.removeNotificationListener(name, listener);
            } finally {
                resetClassLoader(old);
            }
        }

        public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback)
                throws InstanceNotFoundException, ListenerNotFoundException {
            ClassLoader old = pushClassLoader(name);
            try {
                delegate.removeNotificationListener(name, listener, filter, handback);
            } finally {
                resetClassLoader(old);
            }
        }

        public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException {
            ClassLoader old = pushClassLoader(name);
            try {
                delegate.removeNotificationListener(name, listener);
            } finally {
                resetClassLoader(old);
            }
        }

        public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException,
                InvalidAttributeValueException, MBeanException, ReflectionException {
            ClassLoader old = pushClassLoader(name);
            try {
                delegate.setAttribute(name, attribute);
            } finally {
                resetClassLoader(old);
            }
        }

        public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException {
            ClassLoader old = pushClassLoader(name);
            try {
                return delegate.setAttributes(name, attributes);
            } finally {
                resetClassLoader(old);
            }
        }

        public void unregisterMBean(ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException {
            delegate.unregisterMBean(name);
        }

        private ClassLoader pushClassLoader(ObjectName name) throws InstanceNotFoundException {
            ClassLoader mbeanCl = delegate.getClassLoaderFor(name);
            return WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(mbeanCl);
        }

        private ClassLoader pushClassLoaderByName(ObjectName loaderName) throws InstanceNotFoundException {
            ClassLoader mbeanCl = delegate.getClassLoader(loaderName);
            return WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(mbeanCl);
        }

        private void resetClassLoader(ClassLoader cl) {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(cl);
        }
    }

    private static final AuditLoggerInfo NOOP_INFO = new AuditLoggerInfo(AuditLogger.NO_OP_LOGGER);
}
