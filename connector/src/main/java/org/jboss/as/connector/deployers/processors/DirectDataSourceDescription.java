package org.jboss.as.connector.deployers.processors;

import org.jboss.as.ee.component.AbstractComponentDescription;
import org.jboss.as.ee.component.BindingDescription;
import org.jboss.as.ee.component.BindingSourceDescription;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ValueManagedObject;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndex;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.modules.Module;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.value.Values;


public class DirectDataSourceDescription extends BindingSourceDescription {
    public static final String USER_PROP = "user";
    public static final String URL_PROP = "url";
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

    private String className;
    private String description;
    private String url;

    private String databaseName;
    private String serverName;
    private int portNumber = -1;

    private int loginTimeout = -1;

    private int isolationLevel = -1;
    private boolean transactional;

    private int initialPoolSize = -1;
    private int maxIdleTime = -1;
    private int maxPoolSize = -1;
    private int maxStatements = -1;
    private int minPoolSize = -1;

    private String user;
    private String password;

    private String[] properties;


    @Override
    public void getResourceValue(AbstractComponentDescription componentDescription, BindingDescription referenceDescription,
            ServiceBuilder<?> serviceBuilder, DeploymentPhaseContext phaseContext, Injector<ManagedReferenceFactory> injector) {
        final Module module = phaseContext.getDeploymentUnit().getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final DeploymentReflectionIndex index = phaseContext.getDeploymentUnit().getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);


        Object object;
        ClassReflectionIndex<?> classIndex;
        try {
            Class<?> clazz = module.getClassLoader().loadClass(className);
            classIndex = index.getClassIndex(clazz);
            object = classIndex.getConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

        setProperties(classIndex, object);
        injector.inject(new ValueManagedObject(Values.immediateValue(object)));
    }


    private void setProperties(ClassReflectionIndex<?> clazz, Object object) {
        setProperty(clazz, object, DESCRIPTION_PROP, description);
        setProperty(clazz, object, URL_PROP, url);
        setProperty(clazz, object, DATABASE_NAME_PROP, databaseName);
        setProperty(clazz, object, SERVER_NAME_PROP, serverName);
        setProperty(clazz, object, PORT_NUMBER_PROP, portNumber);
        setProperty(clazz, object, LOGIN_TIMEOUT_PROP, loginTimeout);
        setProperty(clazz, object, ISOLATION_LEVEL_PROP, isolationLevel);
        setProperty(clazz, object, TRANSACTIONAL_PROP, transactional);
        setProperty(clazz, object, INITIAL_POOL_SIZE_PROP, initialPoolSize);
        setProperty(clazz, object, MAX_IDLE_TIME_PROP, maxIdleTime);
        setProperty(clazz, object, MAX_POOL_SIZE_PROP, maxPoolSize);
        setProperty(clazz, object, MAX_STATEMENTS_PROP, maxStatements);
        setProperty(clazz, object, MIN_POOL_SIZE_PROP, minPoolSize);
        setProperty(clazz, object, USER_PROP, user);
        setProperty(clazz, object, PASSWORD_PROP, password);

        if (properties != null) for (String property : properties) {
            int pos = property.indexOf('=');
            if (pos == -1 || pos == property.length() - 1) continue;

            setProperty(clazz, object, property.substring(0, pos), property.substring(pos + 1));
        }
    }


    private void setProperty(ClassReflectionIndex<?> clazz, Object object, String name, Object value) {
        // Ignore defaulted values
        if (value == null) return;
        if (value instanceof String && "".equals(value)) return;
        if (value instanceof Integer && ((Integer)value).intValue() == -1) return;
        StringBuilder builder = new StringBuilder("set").append(name);
        builder.setCharAt(3, Character.toUpperCase(name.charAt(0)));
        try {
            clazz.getMethod(void.class, builder.toString(), value.getClass()).invoke(object, value);
        } catch (Exception ignore) {
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
