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

import static org.jboss.as.jmx.MBeanServerSignature.ADD_NOTIFICATION_LISTENER;
import static org.jboss.as.jmx.MBeanServerSignature.CREATE_MBEAN;
import static org.jboss.as.jmx.MBeanServerSignature.DESERIALIZE;
import static org.jboss.as.jmx.MBeanServerSignature.GET_ATTRIBUTE;
import static org.jboss.as.jmx.MBeanServerSignature.GET_ATTRIBUTES;
import static org.jboss.as.jmx.MBeanServerSignature.GET_CLASSLOADER;
import static org.jboss.as.jmx.MBeanServerSignature.GET_CLASSLOADER_FOR;
import static org.jboss.as.jmx.MBeanServerSignature.GET_CLASSLOADER_REPOSITORY;
import static org.jboss.as.jmx.MBeanServerSignature.GET_MBEAN_COUNT;
import static org.jboss.as.jmx.MBeanServerSignature.GET_MBEAN_INFO;
import static org.jboss.as.jmx.MBeanServerSignature.GET_OBJECT_INSTANCE;
import static org.jboss.as.jmx.MBeanServerSignature.INSTANTIATE;
import static org.jboss.as.jmx.MBeanServerSignature.INVOKE;
import static org.jboss.as.jmx.MBeanServerSignature.IS_INSTANCE_OF;
import static org.jboss.as.jmx.MBeanServerSignature.IS_REGISTERED;
import static org.jboss.as.jmx.MBeanServerSignature.QUERY_MBEANS;
import static org.jboss.as.jmx.MBeanServerSignature.QUERY_NAMES;
import static org.jboss.as.jmx.MBeanServerSignature.REGISTER_MBEAN;
import static org.jboss.as.jmx.MBeanServerSignature.REMOVE_NOTIFICATION_LISTENER;
import static org.jboss.as.jmx.MBeanServerSignature.SET_ATTRIBUTE;
import static org.jboss.as.jmx.MBeanServerSignature.SET_ATTRIBUTES;
import static org.jboss.as.jmx.MBeanServerSignature.UNREGISTER_MBEAN;

import java.io.ObjectInputStream;
import java.net.InetAddress;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
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
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
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
import javax.security.auth.Subject;

import org.jboss.as.controller.access.AuthorizationResult;
import org.jboss.as.controller.access.AuthorizationResult.Decision;
import org.jboss.as.controller.access.Caller;
import org.jboss.as.controller.access.JmxAction;
import org.jboss.as.controller.access.management.JmxAuthorizer;
import org.jboss.as.controller.audit.AuditLogger;
import org.jboss.as.controller.audit.ManagedAuditLogger;
import org.jboss.as.controller.security.AccessMechanismPrincipal;
import org.jboss.as.controller.security.InetAddressPrincipal;
import org.jboss.as.core.security.AccessMechanism;
import org.jboss.as.core.security.RealmUser;
import org.jboss.as.server.jmx.MBeanServerPlugin;
import org.jboss.as.server.jmx.PluggableMBeanServer;

/**
 * An MBeanServer supporting {@link MBeanServerPlugin}s. At it's core is the original platform mbean server wrapped in TCCL behaviour.
 * <em>Note:</em> If this class name changes ModelCombiner must be updated.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class PluggableMBeanServerImpl implements PluggableMBeanServer {

    private static final Object[] NO_ARGS = new Object[0];
    private static final String[] EMPTY_SIG = new String[0];

    private final MBeanServerPlugin rootMBeanServer;
    private volatile ManagedAuditLogger auditLogger;

    private final Set<MBeanServerPlugin> delegates = new CopyOnWriteArraySet<MBeanServerPlugin>();

    private volatile JmxAuthorizer authorizer;

    PluggableMBeanServerImpl(MBeanServer rootMBeanServer) {
        this.rootMBeanServer = new TcclMBeanServer(rootMBeanServer);
    }

    void setAuditLogger(ManagedAuditLogger auditLoggerInfo) {
        this.auditLogger = auditLoggerInfo != null ? auditLoggerInfo : AuditLogger.NO_OP_LOGGER;
    }

    void setAuthorizer(JmxAuthorizer authorizer) {
        this.authorizer = authorizer;
    }

    void setNonFacadeMBeansSensitive(boolean sensitive) {
        authorizer.setNonFacadeMBeansSensitive(sensitive);
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
        final boolean readOnly = true;
        try {
            delegate = findDelegate(name);
            if (delegate.shouldAuthorize()) {
                authorizeSensitiveOperation(ADD_NOTIFICATION_LISTENER, readOnly, true);
            }
            delegate.addNotificationListener(name, listener, filter, handback);
        } catch (Exception e) {
            error = e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            if (e instanceof RuntimeException) throw (RuntimeException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).addNotificationListener(name, listener, filter, handback);
            }
        }
    }

    @Override
    public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback)
            throws InstanceNotFoundException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        final boolean readOnly = true;
        try {
            delegate = findDelegate(name);
            if (delegate.shouldAuthorize()) {
                authorizeSensitiveOperation(ADD_NOTIFICATION_LISTENER, readOnly, true);
            }
            delegate.addNotificationListener(name, listener, filter, handback);
        } catch (Exception e) {
            error = e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            if (e instanceof RuntimeException) throw (RuntimeException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).addNotificationListener(name, listener, filter, handback);
            }
        }
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature)
            throws ReflectionException, InstanceAlreadyExistsException, MBeanException,
            NotCompliantMBeanException {
        params = nullAsEmpty(params);
        signature = nullAsEmpty(signature);
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        final boolean readOnly = false;
        try {
            delegate = findDelegateForNewObject(name);
            if (delegate.shouldAuthorize()) {
                authorizeSensitiveOperation(CREATE_MBEAN, readOnly, true);
            }
            return delegate.createMBean(className, name, params, signature);
        } catch (Exception e) {
            error = e;
            if (e instanceof ReflectionException) throw (ReflectionException)e;
            if (e instanceof InstanceAlreadyExistsException) throw (InstanceAlreadyExistsException)e;
            if (e instanceof MBeanException) throw (MBeanException)e;
            if (e instanceof NotCompliantMBeanException) throw (NotCompliantMBeanException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).createMBean(className, name, params, signature);
            }
        }
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params,
            String[] signature) throws ReflectionException, InstanceAlreadyExistsException,
            MBeanException, NotCompliantMBeanException, InstanceNotFoundException {
        params = nullAsEmpty(params);
        signature = nullAsEmpty(signature);
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        final boolean readOnly = false;
        try {
            delegate = findDelegate(name);
            if (delegate.shouldAuthorize()) {
                authorizeSensitiveOperation(CREATE_MBEAN, readOnly, true);
            }
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
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).createMBean(className, name, params, signature);
            }
        }
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName) throws ReflectionException,
            InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException,
            InstanceNotFoundException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        final boolean readOnly = false;
        try {
            delegate = findDelegate(name);
            if (delegate.shouldAuthorize()) {
                authorizeSensitiveOperation(CREATE_MBEAN, readOnly, true);
            }
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
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).createMBean(className, name, loaderName);
            }
        }
    }

    @Override
    public ObjectInstance createMBean(String className, ObjectName name) throws ReflectionException,
            InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        final boolean readOnly = false;
        try {
            delegate = findDelegateForNewObject(name);
            if (delegate.shouldAuthorize()) {
                authorizeSensitiveOperation(CREATE_MBEAN, readOnly, true);
            }
            return delegate.createMBean(className, name);
        } catch (Exception e) {
            error = e;
            if (e instanceof ReflectionException) throw (ReflectionException)e;
            if (e instanceof InstanceAlreadyExistsException) throw (InstanceAlreadyExistsException)e;
            if (e instanceof MBeanException) throw (MBeanException)e;
            if (e instanceof NotCompliantMBeanException) throw (NotCompliantMBeanException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).createMBean(className, name);
            }
        }
    }

    @Override
    @Deprecated
    public ObjectInputStream deserialize(ObjectName name, byte[] data) throws OperationsException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        final boolean readOnly = true;
        try {
            delegate = findDelegate(name);
            if (delegate.shouldAuthorize()) {
                //Special authorization
                authorizeSuperUserOrAdministrator(DESERIALIZE);
            }
            return delegate.deserialize(name, data);
        } catch (Exception e) {
            error = e;
            if (e instanceof OperationsException) throw (OperationsException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).deserialize(name, data);
            }
        }
    }

    @Override
    @Deprecated
    public ObjectInputStream deserialize(String className, byte[] data) throws OperationsException, ReflectionException {
        Throwable error = null;
        MBeanServerPlugin delegate = rootMBeanServer;
        final boolean readOnly = true;
        try {
            if (delegate.shouldAuthorize()) {
                //Special authorization
                authorizeSuperUserOrAdministrator(DESERIALIZE);
            }
            return delegate.deserialize(className, data);
        } catch (Exception e) {
            error = e;
            if (e instanceof OperationsException) throw (OperationsException)e;
            if (e instanceof ReflectionException) throw (ReflectionException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).deserialize(className, data);
            }
        }
    }

    @Override
    @Deprecated
    public ObjectInputStream deserialize(String className, ObjectName loaderName, byte[] data) throws OperationsException, ReflectionException {
        Throwable error = null;
        MBeanServerPlugin delegate = rootMBeanServer;
        final boolean readOnly = true;
        try {
            if (delegate.shouldAuthorize()) {
                //Special authorization
                authorizeSuperUserOrAdministrator(DESERIALIZE);
            }
            return delegate.deserialize(className, loaderName, data);
        } catch (Exception e) {
            error = e;
            if (e instanceof OperationsException) throw (OperationsException)e;
            if (e instanceof ReflectionException) throw (ReflectionException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).deserialize(className, loaderName, data);
            }
        }
    }

    @Override
    public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException,
            InstanceNotFoundException, ReflectionException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        final boolean readOnly = true;
        try {
            delegate = findDelegate(name);
            if (delegate.shouldAuthorize()) {
                authorizeSensitiveOperation(GET_ATTRIBUTE, readOnly, true);
            }
            return delegate.getAttribute(name, attribute);
        } catch (Exception e) {
            error = e;
            if (e instanceof MBeanException) throw (MBeanException)e;
            if (e instanceof AttributeNotFoundException) throw (AttributeNotFoundException)e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            if (e instanceof ReflectionException) throw (ReflectionException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).getAttribute(name, attribute);
            }
        }
    }

    @Override
    public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException,
            ReflectionException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        final boolean readOnly = true;
        try {
            delegate = findDelegate(name);
            if (delegate.shouldAuthorize()) {
                authorizeSensitiveOperation(GET_ATTRIBUTES, readOnly, true);
            }
            return delegate.getAttributes(name, attributes);
        } catch (Exception e) {
            error = e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            if (e instanceof ReflectionException) throw (ReflectionException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).getAttributes(name, attributes);
            }
        }
    }

    @Override
    public ClassLoader getClassLoader(ObjectName loaderName) throws InstanceNotFoundException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        final boolean readOnly = true;
        try {
            delegate = findDelegate(loaderName);
            if (delegate.shouldAuthorize()) {
                //Special authorization
                authorizeSuperUserOrAdministrator(GET_CLASSLOADER);
            }
            return delegate.getClassLoader(loaderName);
        } catch (Exception e) {
            error = e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).getClassLoader(loaderName);
            }
        }
    }

    @Override
    public ClassLoader getClassLoaderFor(ObjectName mbeanName) throws InstanceNotFoundException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        final boolean readOnly = true;
        try {
            delegate = findDelegate(mbeanName);
            if (delegate.shouldAuthorize()) {
                //Special authorization
                authorizeSuperUserOrAdministrator(GET_CLASSLOADER_FOR);
            }
            return delegate.getClassLoaderFor(mbeanName);
        } catch (Exception e) {
            error = e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).getClassLoaderFor(mbeanName);
            }
        }
    }

    @Override
    public ClassLoaderRepository getClassLoaderRepository() {
        Throwable error = null;
        MBeanServerPlugin delegate = rootMBeanServer;
        final boolean readOnly = true;
        try {
            if (delegate.shouldAuthorize()) {
                //Special authorization
                authorizeSuperUserOrAdministrator(GET_CLASSLOADER_REPOSITORY);
            }
            return delegate.getClassLoaderRepository();
        } catch (Exception e) {
            error = e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).getClassLoaderRepository();
            }
        }
    }

    @Override
    public String getDefaultDomain() {
        Throwable error = null;
        MBeanServerPlugin delegate = rootMBeanServer;
        final boolean readOnly = true;
        try {
            //No authorization needed to get the name of the default domain
            return delegate.getDefaultDomain();
        } catch (Exception e) {
            error = e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).getDefaultDomain();
            }
        }
    }

    @Override
    public String[] getDomains() {
        Throwable error = null;
        final boolean readOnly = true;
        try {
            //No authorization needed to get the names of the domains
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
            new MBeanServerAuditLogRecordFormatter(this, error, readOnly).getDomains();
        }
    }

    @Override
    public Integer getMBeanCount() {
        Throwable error = null;
        final boolean readOnly = true;
        boolean shouldLog = false;
        try {
            int i = 0;
            if (delegates.size() > 0) {
                for (MBeanServerPlugin delegate : delegates) {
                    //Only include the count if the user is authorized to see the beans in the domain
                    if (!delegate.shouldAuthorize() || authorizeSensitiveOperation(GET_MBEAN_COUNT, true, false)) {
                        i += delegate.getMBeanCount();
                        if (delegate.shouldAuditLog()) {
                            shouldLog = true;
                        }
                    }
                }
            }
            if (!rootMBeanServer.shouldAuthorize() || authorizeSensitiveOperation(GET_MBEAN_COUNT, true, false)) {
                //Only include the count if the user is authorized to see the beans in the domain
                i += rootMBeanServer.getMBeanCount();
                shouldLog = true;
            }
            return i;
        } catch (Exception e) {
            error = e;
            throw makeRuntimeException(e);
        } finally {
            if (error != null || shouldLog) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).getMBeanCount();
            }
        }
    }

    @Override
    public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException {
        return getMBeanInfo(name, true, false);
    }

    private MBeanInfo getMBeanInfo(ObjectName name, boolean logAndAuthorize, boolean nullIfNotFound) throws InstanceNotFoundException, IntrospectionException, ReflectionException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        final boolean readOnly = true;
        try {
            delegate = findDelegate(name);
            if (delegate.shouldAuthorize() && logAndAuthorize) {
                authorizeSensitiveOperation(GET_MBEAN_INFO, readOnly, true);
            }
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
            if (logAndAuthorize && shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).getMBeanInfo(name);
            }
        }
    }

    @Override
    public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        final boolean readOnly = true;
        try {
            delegate = findDelegate(name);
            if (delegate.shouldAuthorize()) {
                authorizeSensitiveOperation(GET_OBJECT_INSTANCE, readOnly, true);
            }
            return delegate.getObjectInstance(name);
        } catch (Exception e) {
            error = e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).getObjectInstance(name);
            }
        }
    }

    @Override
    public Object instantiate(String className, Object[] params, String[] signature) throws ReflectionException, MBeanException {
        params = nullAsEmpty(params);
        signature = nullAsEmpty(signature);
        Throwable error = null;
        MBeanServerPlugin delegate = rootMBeanServer;
        final boolean readOnly = false;
        try {
            if (delegate.shouldAuthorize()) {
                authorizeSuperUserOrAdministrator(INSTANTIATE);
            }
            return delegate.instantiate(className, params, signature);
        } catch (Exception e) {
            error = e;
            if (e instanceof ReflectionException) throw (ReflectionException)e;
            if (e instanceof MBeanException) throw (MBeanException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).instantiate(className, params, signature);
            }
        }
    }

    @Override
    public Object instantiate(String className, ObjectName loaderName, Object[] params, String[] signature)
            throws ReflectionException, MBeanException, InstanceNotFoundException {
        params = nullAsEmpty(params);
        signature = nullAsEmpty(signature);
        Throwable error = null;
        MBeanServerPlugin delegate = rootMBeanServer;
        final boolean readOnly = false;
        try {
            if (delegate.shouldAuthorize()) {
                authorizeSuperUserOrAdministrator(INSTANTIATE);
            }
            return delegate.instantiate(className, loaderName, params, signature);
        } catch (Exception e) {
            error = e;
            if (e instanceof ReflectionException) throw (ReflectionException)e;
            if (e instanceof MBeanException) throw (MBeanException)e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).instantiate(className, loaderName, params, signature);
            }
        }
    }

    @Override
    public Object instantiate(String className, ObjectName loaderName) throws ReflectionException, MBeanException,
            InstanceNotFoundException {
        Throwable error = null;
        MBeanServerPlugin delegate = rootMBeanServer;
        final boolean readOnly = false;
        try {
            if (delegate.shouldAuthorize()) {
                authorizeSuperUserOrAdministrator(INSTANTIATE);
            }
            return delegate.instantiate(className, loaderName);
        } catch (Exception e) {
            error = e;
            if (e instanceof ReflectionException) throw (ReflectionException)e;
            if (e instanceof MBeanException) throw (MBeanException)e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).instantiate(className, loaderName);
            }
        }
    }

    @Override
    public Object instantiate(String className) throws ReflectionException, MBeanException {
        Throwable error = null;
        MBeanServerPlugin delegate = rootMBeanServer;
        final boolean readOnly = false;
        try {
            if (delegate.shouldAuthorize()) {
                authorizeSuperUserOrAdministrator(INSTANTIATE);
            }
            return delegate.instantiate(className);
        } catch (Exception e) {
            error = e;
            if (e instanceof ReflectionException) throw (ReflectionException)e;
            if (e instanceof MBeanException) throw (MBeanException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).instantiate(className);
            }
        }
    }

    @Override
    public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature)
            throws InstanceNotFoundException, MBeanException, ReflectionException {
        params = nullAsEmpty(params);
        signature = nullAsEmpty(signature);
        Throwable error = null;
        MBeanServerPlugin delegate = findDelegate(name);
        boolean readOnly = false;
        try {
            //Need to determine impact of the operation
            readOnly = isOperationReadOnly(name, operationName, signature);
            delegate = findDelegate(name);
            if (delegate.shouldAuthorize()) {
                authorizeSensitiveOperation(INVOKE, readOnly, true);
            }
            return delegate.invoke(name, operationName, params, signature);
        } catch (Exception e) {
            error = e;
            if (e instanceof ReflectionException) throw (ReflectionException)e;
            if (e instanceof MBeanException) throw (MBeanException)e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).invoke(name, operationName, params, signature);
            }
        }
    }

    @Override
    public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        final boolean readOnly = true;
        try {
            delegate = findDelegate(name);
            if (delegate.shouldAuthorize()) {
                authorizeSensitiveOperation(IS_INSTANCE_OF, readOnly, true);
            }
            return delegate.isInstanceOf(name, className);
        } catch (Exception e) {
            error = e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).isInstanceOf(name, className);
            }
        }
    }

    @Override
    public boolean isRegistered(ObjectName name) {
        Throwable error = null;
        Boolean shouldAuditLog = null;
        final boolean readOnly = true;
        try {
            if (delegates.size() > 0) {
                for (MBeanServerPlugin delegate : delegates) {
                    if (delegate.accepts(name) && delegate.isRegistered(name)) {
                        if (delegate.shouldAuthorize()) {
                            authorizeSensitiveOperation(IS_REGISTERED, readOnly, true);
                        }
                        if (delegate.shouldAuditLog()) {
                            shouldAuditLog = true;
                        }
                        return true;
                    }
                }
            }
            // check if it's registered with the root (a.k.a platform) MBean server
            shouldAuditLog = true;
            if (rootMBeanServer.shouldAuthorize()) {
                authorizeSensitiveOperation(IS_REGISTERED, readOnly, true);
            }
            return rootMBeanServer.isRegistered(name);
        } catch (Exception e) {
            error = e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog == null || shouldAuditLog) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).isRegistered(name);
            }
        }

    }

    @Override
    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) {
        Throwable error = null;
        final boolean readOnly = true;
        boolean shouldAuditLog = false;
        try {
            Set<ObjectInstance> result = new HashSet<ObjectInstance>();
            if (delegates.size() > 0) {
                for (MBeanServerPlugin delegate : delegates) {
                    if (name == null || (name.getDomain() != null && delegate.accepts(name))) {
                        //Only include the mbeans if the user is authorized to see the beans in the domain
                        if (!delegate.shouldAuthorize() || authorizeSensitiveOperation(QUERY_MBEANS, true, false)) {
                            result.addAll(delegate.queryMBeans(name, query));
                            if (delegate.shouldAuditLog()) {
                                shouldAuditLog = true;
                            }
                        }
                    }
                }
            }
            //Only include the mbeans if the user is authorized to see the beans in the domain
            if (!rootMBeanServer.shouldAuthorize() || authorizeSensitiveOperation(QUERY_MBEANS, true, false)) {
                result.addAll(rootMBeanServer.queryMBeans(name, query));
                shouldAuditLog = true;
            }
            return result;
        } catch (Exception e) {
            error = e;
            throw makeRuntimeException(e);
        } finally {
            if (error != null || shouldAuditLog) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).queryMBeans(name, query);
            }
        }
    }

    @Override
    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) {
        Throwable error = null;
        final boolean readOnly = true;
        boolean shouldAuditLog = false;
        try {
            Set<ObjectName> result = new HashSet<ObjectName>();
            if (delegates.size() > 0) {
                for (MBeanServerPlugin delegate : delegates) {
                    if (name == null || (name.getDomain() != null && delegate.accepts(name))) {
                        //Only include the mbeans if the user is authorized to see the beans in the domain
                        if (!delegate.shouldAuthorize() || authorizeSensitiveOperation(QUERY_NAMES, true, false)) {
                            result.addAll(delegate.queryNames(name, query));
                            if (delegate.shouldAuditLog()) {
                                shouldAuditLog = true;
                            }
                        }
                    }
                }
            }
            //Only include the mbeans if the user is authorized to see the beans in the domain
            if (!rootMBeanServer.shouldAuthorize() || authorizeSensitiveOperation(QUERY_NAMES, true, false)) {
                result.addAll(rootMBeanServer.queryNames(name, query));
                shouldAuditLog = true;
            }
            return result;
        } catch (Exception e) {
            error = e;
            throw makeRuntimeException(e);
        } finally {
            if (error != null || shouldAuditLog) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).queryNames(name, query);
            }
        }
    }

    @Override
    public ObjectInstance registerMBean(Object object, ObjectName name) throws InstanceAlreadyExistsException,
            MBeanRegistrationException, NotCompliantMBeanException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        final boolean readOnly = false;
        try {
            delegate = findDelegateForNewObject(name);
            if (delegate.shouldAuthorize()) {
                authorizeSensitiveOperation(REGISTER_MBEAN, readOnly, true);
            }
            return delegate.registerMBean(object, name);
        } catch (Exception e) {
            error = e;
            if (e instanceof InstanceAlreadyExistsException) throw (InstanceAlreadyExistsException)e;
            if (e instanceof MBeanRegistrationException) throw (MBeanRegistrationException)e;
            if (e instanceof NotCompliantMBeanException) throw (NotCompliantMBeanException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).registerMBean(object, name);
            }
        }
    }

    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter,
            Object handback) throws InstanceNotFoundException, ListenerNotFoundException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        final boolean readOnly = true;
        try {
            delegate = findDelegate(name);
            if (delegate.shouldAuthorize()) {
                authorizeSensitiveOperation(REMOVE_NOTIFICATION_LISTENER, readOnly, true);
            }
            delegate.removeNotificationListener(name, listener, filter, handback);
        } catch (Exception e) {
            error = e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            if (e instanceof ListenerNotFoundException) throw (ListenerNotFoundException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).removeNotificationListener(name, listener, filter, handback);
            }
        }
    }

    @Override
    public void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException,
            ListenerNotFoundException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        final boolean readOnly = true;
        try {
            delegate = findDelegate(name);
            if (delegate.shouldAuthorize()) {
                authorizeSensitiveOperation(REMOVE_NOTIFICATION_LISTENER, readOnly, true);
            }
            delegate.removeNotificationListener(name, listener);
        } catch (Exception e) {
            error = e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            if (e instanceof ListenerNotFoundException) throw (ListenerNotFoundException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).removeNotificationListener(name, listener);
            }
        }
    }

    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback)
            throws InstanceNotFoundException, ListenerNotFoundException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        final boolean readOnly = true;
        try {
            delegate = findDelegate(name);
            if (delegate.shouldAuthorize()) {
                authorizeSensitiveOperation(REMOVE_NOTIFICATION_LISTENER, readOnly, true);
            }
            delegate.removeNotificationListener(name, listener, filter, handback);
        } catch (Exception e) {
            error = e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            if (e instanceof ListenerNotFoundException) throw (ListenerNotFoundException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).removeNotificationListener(name, listener, filter, handback);
            }
        }
    }

    @Override
    public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException,
            ListenerNotFoundException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        final boolean readOnly = true;
        try {
            delegate = findDelegate(name);
            if (delegate.shouldAuthorize()) {
                authorizeSensitiveOperation(REMOVE_NOTIFICATION_LISTENER, readOnly, true);
            }
            delegate.removeNotificationListener(name, listener);
        } catch (Exception e) {
            error = e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            if (e instanceof ListenerNotFoundException) throw (ListenerNotFoundException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).removeNotificationListener(name, listener);
            }
        }
    }

    @Override
    public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException,
            InvalidAttributeValueException, MBeanException, ReflectionException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        final boolean readOnly = false;
        try {
            delegate = findDelegate(name);
            if (delegate.shouldAuthorize()) {
                authorizeSensitiveOperation(SET_ATTRIBUTE, readOnly, true);
            }
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
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).setAttribute(name, attribute);
            }
        }
    }

    @Override
    public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException,
            ReflectionException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        final boolean readOnly = false;
        try {
            delegate = findDelegate(name);
            if (delegate.shouldAuthorize()) {
                authorizeSensitiveOperation(SET_ATTRIBUTES, readOnly, true);
            }
            return delegate.setAttributes(name, attributes);
        } catch (Exception e) {
            error = e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            if (e instanceof ReflectionException) throw (ReflectionException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).setAttributes(name, attributes);
            }
        }
    }

    @Override
    public void unregisterMBean(ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException {
        Throwable error = null;
        MBeanServerPlugin delegate = null;
        final boolean readOnly = false;
        try {
            delegate = findDelegate(name);
            if (delegate.shouldAuthorize()) {
                authorizeSensitiveOperation(UNREGISTER_MBEAN, readOnly, true);
            }
            delegate.unregisterMBean(name);
        } catch (Exception e) {
            error = e;
            if (e instanceof InstanceNotFoundException) throw (InstanceNotFoundException)e;
            if (e instanceof MBeanRegistrationException) throw (MBeanRegistrationException)e;
            throw makeRuntimeException(e);
        } finally {
            if (shouldAuditLog(delegate, readOnly)) {
                new MBeanServerAuditLogRecordFormatter(this, error, readOnly).unregisterMBean(name);
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

    private boolean shouldAuditLog(MBeanServerPlugin delegate, boolean readOnly) {
        if (auditLogger != null) {
            if (delegate == null) {
                return true;
            }
            return delegate.shouldAuditLog();
        }
        return false;
    }

    private RuntimeException makeRuntimeException(Exception e) {
        if (e instanceof RuntimeException) {
            return (RuntimeException)e;
        }
        return new RuntimeException(e);
    }

    private boolean isOperationReadOnly(ObjectName name, String operationName, String[] signature) {
        MBeanInfo info;
        try {
            info = getMBeanInfo(name, false, true);
        } catch (Exception e) {
            //This should not happen, just in case say it is not RO
            return false;
        }
        if (info == null) {
            //Default to not RO
            return false;
        }
        for (MBeanOperationInfo op : info.getOperations()) {
            if (op.getName().equals(operationName)) {
                MBeanParameterInfo[] params = op.getSignature();
                if (params.length != signature.length) {
                    continue;
                }
                boolean same = true;
                for (int i = 0 ; i < params.length ; i++) {
                    if (!params[i].getType().equals(signature[i])) {
                        same = false;
                        break;
                    }
                }
                if (same) {
                    return op.getImpact() == MBeanOperationInfo.INFO;
                }
            }
        }
        //Default to not RO
        return false;
    }

    void log(boolean readOnly, Throwable error, String methodName, String[] methodSignature, Object...methodParams) {
        if (System.getSecurityManager() != null) {
            AccessController.doPrivileged(new LogAction(auditLogger, readOnly, error, methodName, methodSignature, methodParams));
        } else {
            LogAction.doLog(auditLogger, readOnly, error, methodName, methodSignature, methodParams);
        }
    }

    boolean authorizeSensitiveOperation(String methodName, boolean readOnly, boolean exception) throws MBeanException {
        if (authorizer != null) {
            final JmxAction target = new JmxAction(methodName, readOnly ? JmxAction.Impact.READ_ONLY : JmxAction.Impact.WRITE);
            //TODO populate the 'environment' variable
            AuthorizationResult authorizationResult = authorizer.authorizeJmxOperation(getCaller(), null, target);
            if (authorizationResult.getDecision() != Decision.PERMIT) {
                if (exception) {
                    throw JmxMessages.MESSAGES.unauthorized();
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    boolean authorizeSuperUserOrAdministrator(String methodName) throws MBeanException {
        if (authorizer != null) {
            //TODO populate the 'environment' variable
            AuthorizationResult authorizationResult = authorizer.authorizeJmxOperation(getCaller(), null, new JmxAction(methodName, JmxAction.Impact.EXTRA_SENSITIVE));
            if (authorizationResult.getDecision() != Decision.PERMIT) {
                throw JmxMessages.MESSAGES.unauthorized();
            }
        }
        return true;
    }

    private Caller getCaller() {
        if (System.getSecurityManager() != null) {
            return AccessController.doPrivileged(GET_CALLER_ACTION);
        } else {
            return GET_CALLER_ACTION.run();
        }
    }


    private String[] nullAsEmpty(String[] array) {
        if (array == null) {
            return EMPTY_SIG;
        }
        return array;
    }

    private Object[] nullAsEmpty(Object[] array) {
        if (array == null) {
            return NO_ARGS;
        }
        return array;
    }

    static final class LogAction implements PrivilegedAction<Void> {
        final ManagedAuditLogger auditLogger;
        final boolean readOnly;
        final Throwable error;
        final String methodName;
        final String[] methodSignature;
        final Object[] methodParams;

        public LogAction(ManagedAuditLogger auditLogger, boolean readOnly, Throwable error, String methodName,
                String[] methodSignature, Object[] methodParams) {
            this.auditLogger = auditLogger;
            this.readOnly = readOnly;
            this.error = error;
            this.methodName = methodName;
            this.methodSignature = methodSignature;
            this.methodParams = methodParams;
        }


        @Override
        public Void run() {
            doLog(auditLogger, readOnly, error, methodName, methodSignature, methodParams);
            return null;
        }


        static void doLog(ManagedAuditLogger auditLogger, boolean readOnly, Throwable error, String methodName, String[] methodSignature, Object...methodParams) {
            Subject subject = Subject.getSubject(AccessController.getContext());
            if (auditLogger != null) {
                auditLogger.logJmxMethodAccess(
                        readOnly,
                        getCallerUserId(subject),
                        null, //TODO domainUUID
                        getSubjectAccessMechanism(subject),
                        getSubjectInetAddress(subject),
                        methodName,
                        methodSignature,
                        methodParams,
                        error);
            }
        }

        private static String getCallerUserId(Subject subject) {
            String userId = null;
            if (subject != null) {
                Set<RealmUser> realmUsers = subject.getPrincipals(RealmUser.class);
                RealmUser user = realmUsers.iterator().next();
                userId = user.getName();
            }
            return userId;
        }

        private static InetAddress getSubjectInetAddress(Subject subject) {
            InetAddressPrincipal principal = getPrincipal(subject, InetAddressPrincipal.class);
            return principal != null ? principal.getInetAddress() : null;
        }

        private static AccessMechanism getSubjectAccessMechanism(Subject subject) {
            AccessMechanismPrincipal principal = getPrincipal(subject, AccessMechanismPrincipal.class);
            return principal != null ? principal.getAccessMechanism() : null;
        }


        private static <T extends Principal> T getPrincipal(Subject subject, Class<T> clazz) {
            if (subject == null) {
                return null;
            }
            Set<T> principals = subject.getPrincipals(clazz);
            assert principals.size() <= 1;
            if (principals.size() == 0) {
                return null;
            }
            return principals.iterator().next();
        }
    };

    private static final PrivilegedAction<Caller> GET_CALLER_ACTION = new PrivilegedAction<Caller>() {
        @Override
        public Caller run() {
            Subject subject = Subject.getSubject(AccessController.getContext());
            return Caller.createCaller(subject);
        }
    };


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
            return SecurityActions.setThreadContextClassLoader(mbeanCl);
        }

        private ClassLoader pushClassLoaderByName(ObjectName loaderName) throws InstanceNotFoundException {
            ClassLoader mbeanCl = delegate.getClassLoader(loaderName);
            return SecurityActions.setThreadContextClassLoader(mbeanCl);
        }

        private void resetClassLoader(ClassLoader cl) {
            SecurityActions.resetThreadContextClassLoader(cl);
        }
    }
}
