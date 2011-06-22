/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.datasource;

import org.jboss.as.ee.component.InjectionSource;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndexUtil;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.invocation.proxy.ProxyFactory;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.Values;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * A binding description for DataSourceDefinition annotations.
 * <p/>
 * The referenced datasource must be directly visible to the
 * component declaring the annotation.
 *
 * @author Jason T. Greene
 */
public class DirectDataSourceInjectionSource extends InjectionSource {

    private static final Logger logger = Logger.getLogger(DirectDataSourceInjectionSource.class);

    public static final ServiceName JBOSS_TXN = ServiceName.JBOSS.append("txn");
    public static final ServiceName JBOSS_TXN_TRANSACTION_MANAGER = JBOSS_TXN.append("TransactionManager");
    public static final ServiceName JBOSS_TXN_SYNCHRONIZATION_REGISTRY = JBOSS_TXN.append("TransactionSynchronizationRegistry");

    public static final String USER_PROP = "user";
    public static final String URL_PROP = "url";
    public static final String UPPERCASE_USER_PROP = "URL";
    public static final String TRANSACTIONAL_PROP = "transactional";
    public static final String SERVER_NAME_PROP = "serverName";
    public static final String PROPERTIES_PROP = "properties";
    public static final String PORT_NUMBER_PROP = "portNumber";
    public static final String PASSWORD_PROP = "password";
    public static final String MIN_POOL_SIZE_PROP = "minPoolSize";
    public static final String MAX_STATEMENTS_PROP = "maxStatements";
    public static final String MAX_IDLE_TIME_PROP = "maxIdleTime";
    public static final String LOGIN_TIMEOUT_PROP = "loginTimeout";
    public static final String ISOLATION_LEVEL_PROP = "isolationLevel";
    public static final String INITIAL_POOL_SIZE_PROP = "initialPoolSize";
    public static final String DESCRIPTION_PROP = "description";
    public static final String DATABASE_NAME_PROP = "databaseName";
    public static final String MAX_POOL_SIZE_PROP = "maxPoolSize";

    private static final Class<?>[] NO_CLASSES = new Class<?>[0];

    private String className;
    private String description;
    private String url;

    private String databaseName;
    private String serverName;
    private int portNumber = -1;

    private int loginTimeout = -1;

    private int isolationLevel = -1;
    private boolean transactional = true;

    private int initialPoolSize = -1;
    private int maxIdleTime = -1;
    private int maxPoolSize = -1;
    private int maxStatements = -1;
    private int minPoolSize = -1;

    private String user;
    private String password;

    private String[] properties;

    public void getResourceValue(final ResolutionContext context, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        final Module module = phaseContext.getDeploymentUnit().getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final DeploymentReflectionIndex deploymentReflectionIndex = phaseContext.getDeploymentUnit().getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);


        Object object;
        ClassReflectionIndex<?> classIndex;
        try {
            Class<?> clazz = module.getClassLoader().loadClass(className);
            classIndex = deploymentReflectionIndex.getClassIndex(clazz);
            Constructor<?> ctor = classIndex.getConstructor(NO_CLASSES);
            if (ctor == null) {
                throw new DeploymentUnitProcessingException("Could not found no-arg constructor for @DataSourceDefinition class " + className);
            }
            object = ctor.newInstance();

            setProperties(deploymentReflectionIndex, classIndex, object);


            if (transactional) {

                final ServiceController<?> syncController = phaseContext.getServiceRegistry().getService(JBOSS_TXN_SYNCHRONIZATION_REGISTRY);
                final ServiceController<?> managerController = phaseContext.getServiceRegistry().getService(JBOSS_TXN_TRANSACTION_MANAGER);
                if (syncController == null || managerController == null) {
                    logger.warn("Transactional datasource " + className + " will not be enlisted in the transaction as the transaction subsystem is not available");
                } else {
                    try {
                        TransactionSynchronizationRegistry transactionSynchronizationRegistry = (TransactionSynchronizationRegistry) syncController.getValue();
                        TransactionManager transactionManager = (TransactionManager)managerController.getValue();
                        ProxyFactory<?> proxyFactory = new ProxyFactory(clazz);
                        object = proxyFactory.newInstance(new DataSourceTransactionProxyHandler(object, transactionManager, transactionSynchronizationRegistry));
                    } catch (Exception e) {
                        logger.warn("Transactional datasource " + className + " could not be proxied and will not be enlisted in transactions automatically", e);
                    }
                }
            }
            injector.inject(new ValueManagedReferenceFactory(Values.immediateValue(object)));
        } catch (Exception e) {
            throw new DeploymentUnitProcessingException(e);
        }
    }

    private void setProperties(DeploymentReflectionIndex deploymentReflectionIndex, ClassReflectionIndex<?> classIndex, Object object) {
        setProperty(deploymentReflectionIndex, classIndex, object, DESCRIPTION_PROP, description);
        setProperty(deploymentReflectionIndex, classIndex, object, URL_PROP, url);
        setProperty(deploymentReflectionIndex, classIndex, object, UPPERCASE_USER_PROP, url);
        setProperty(deploymentReflectionIndex, classIndex, object, DATABASE_NAME_PROP, databaseName);
        setProperty(deploymentReflectionIndex, classIndex, object, SERVER_NAME_PROP, serverName);
        setProperty(deploymentReflectionIndex, classIndex, object, PORT_NUMBER_PROP, Integer.valueOf(portNumber));
        setProperty(deploymentReflectionIndex, classIndex, object, LOGIN_TIMEOUT_PROP, Integer.valueOf(loginTimeout));
        setProperty(deploymentReflectionIndex, classIndex, object, ISOLATION_LEVEL_PROP, Integer.valueOf(isolationLevel));
        setProperty(deploymentReflectionIndex, classIndex, object, TRANSACTIONAL_PROP, Boolean.valueOf(transactional));
        setProperty(deploymentReflectionIndex, classIndex, object, INITIAL_POOL_SIZE_PROP, Integer.valueOf(initialPoolSize));
        setProperty(deploymentReflectionIndex, classIndex, object, MAX_IDLE_TIME_PROP, Integer.valueOf(maxIdleTime));
        setProperty(deploymentReflectionIndex, classIndex, object, MAX_POOL_SIZE_PROP, Integer.valueOf(maxPoolSize));
        setProperty(deploymentReflectionIndex, classIndex, object, MAX_STATEMENTS_PROP, Integer.valueOf(maxStatements));
        setProperty(deploymentReflectionIndex, classIndex, object, MIN_POOL_SIZE_PROP, Integer.valueOf(minPoolSize));
        setProperty(deploymentReflectionIndex, classIndex, object, USER_PROP, user);
        setProperty(deploymentReflectionIndex, classIndex, object, PASSWORD_PROP, password);

        if (properties != null) for (String property : properties) {
            int pos = property.indexOf('=');
            if (pos == -1 || pos == property.length() - 1) continue;

            setProperty(deploymentReflectionIndex, classIndex, object, property.substring(0, pos), property.substring(pos + 1));
        }
    }

    private void setProperty(DeploymentReflectionIndex deploymentReflectionIndex, ClassReflectionIndex<?> classIndex, Object object, String name, Object value) {
        // Ignore defaulted values
        if (value == null) return;
        if (value instanceof String && "".equals(value)) return;
        if (value instanceof Integer && ((Integer) value).intValue() == -1) return;
        StringBuilder builder = new StringBuilder("set").append(name);
        builder.setCharAt(3, Character.toUpperCase(name.charAt(0)));
        final String methodName = builder.toString();
        final Class<?> paramType = value.getClass();
        final MethodIdentifier methodIdentifier = MethodIdentifier.getIdentifier(void.class, methodName, paramType);
        final Method setterMethod = ClassReflectionIndexUtil.findMethod(deploymentReflectionIndex, classIndex, methodIdentifier);
        if (setterMethod == null) {
            // just log a WARN message
            logger.warn("Ignoring property " + name + " due to missing setter method: " + methodName + "("
                    + paramType.getName() + ") on datasource class: " + classIndex.getIndexedClass().getName());
            return;
        }
        try {
            setterMethod.invoke(object, value);
        } catch (Exception e) {
            throw new RuntimeException("Could not set property " + name + " on datasource class " + classIndex.getIndexedClass().getName(), e);
        }
    }

    public String getClassName() {
        return className;
    }


    public void setClassName(String className) {
        this.className = className;
    }


    public String getDescription() {
        return description;
    }


    public void setDescription(String description) {
        this.description = description;
    }


    public String getUrl() {
        return url;
    }


    public void setUrl(String url) {
        this.url = url;
    }


    public String getDatabaseName() {
        return databaseName;
    }


    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }


    public String getServerName() {
        return serverName;
    }


    public void setServerName(String serverName) {
        this.serverName = serverName;
    }


    public int getPortNumber() {
        return portNumber;
    }


    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }


    public int getLoginTimeout() {
        return loginTimeout;
    }


    public void setLoginTimeout(int loginTimeout) {
        this.loginTimeout = loginTimeout;
    }


    public int getIsolationLevel() {
        return isolationLevel;
    }


    public void setIsolationLevel(int isolationLevel) {
        this.isolationLevel = isolationLevel;
    }


    public boolean isTransactional() {
        return transactional;
    }


    public void setTransactional(boolean transactional) {
        this.transactional = transactional;
    }


    public int getInitialPoolSize() {
        return initialPoolSize;
    }


    public void setInitialPoolSize(int initialPoolSize) {
        this.initialPoolSize = initialPoolSize;
    }


    public int getMaxIdleTime() {
        return maxIdleTime;
    }


    public void setMaxIdleTime(int maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }


    public int getMaxPoolSize() {
        return maxPoolSize;
    }


    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }


    public int getMaxStatements() {
        return maxStatements;
    }


    public void setMaxStatements(int maxStatements) {
        this.maxStatements = maxStatements;
    }


    public int getMinPoolSize() {
        return minPoolSize;
    }


    public void setMinPoolSize(int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }


    public String getUser() {
        return user;
    }


    public void setUser(String user) {
        this.user = user;
    }


    public String getPassword() {
        return password;
    }


    public void setPassword(String password) {
        this.password = password;
    }


    public String[] getProperties() {
        return properties;
    }


    public void setProperties(String[] properties) {
        this.properties = properties;
    }

}
