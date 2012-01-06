/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.arquillian.container;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

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
import javax.management.MBeanServerConnection;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;

/**
 * A HORRIBLE, HORRIBLE HACK!  Tunnels JMX calls over a special management op.
 * Notifications are mapped to ugly file polling.
 *
 * <p>This should be replaced with a proper JMX over remoting protocol.
 *
 * @author Jason T. Greene
 */
public class TunneledMBeanServerConnection implements MBeanServerConnection {
    private static final String INVOKE_MBEAN_RAW = "invoke-mbean-raw";
    private static final String GET_MBEAN_INFO_RAW ="get-mbean-info-raw";
    private static final String GET_MBEAN_ATTRIBUTE_INFO_RAW = "get-mbean-attribute-info-raw";
    private static final String JMX = "jmx";
    private static final String MBEAN_NAME = "mbean-name";
    private static final String MBEAN_ATTRIBUTE_NAME = "mbean-attribute-name";
    private static final String MBEAN_OPERATION_NAME = "mbean-operation-name";
    private static final String NOTIFICATION_FILE = "notification-file";
    private static final String SIGNATURE = "signature";
    private static final String PARAMETERS = "parameters";

    private CopyOnWriteArrayList<NotificationListener> listeners = new CopyOnWriteArrayList<NotificationListener>();
    private ModelControllerClient client;

    public TunneledMBeanServerConnection(ModelControllerClient client) {
        this.client = client;
    }

    public ObjectInstance createMBean(String className, ObjectName name) throws ReflectionException, InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException, IOException {
        return null;
    }

    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName) throws ReflectionException, InstanceAlreadyExistsException,  MBeanException, NotCompliantMBeanException, InstanceNotFoundException, IOException {
        return null;
    }

    public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException, IOException {
        return null;
    }

    public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params, String[] signature) throws ReflectionException, InstanceAlreadyExistsException, MBeanException, NotCompliantMBeanException, InstanceNotFoundException, IOException {
        return null;
    }

    public void unregisterMBean(ObjectName name) throws InstanceNotFoundException, MBeanRegistrationException, IOException {
    }

    public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException, IOException {
        return null;
    }

    public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) throws IOException {
        return null;
    }

    public Set<ObjectName> queryNames(ObjectName name, QueryExp query) throws IOException {
        return null;
    }

    public boolean isRegistered(ObjectName name) throws IOException {
        return false;
    }

    public Integer getMBeanCount() throws IOException {
        return null;
    }

    public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException, InstanceNotFoundException, ReflectionException, IOException {
        ModelNode request = new ModelNode();
        request.get(OP).set(GET_MBEAN_ATTRIBUTE_INFO_RAW);
        request.get(OP_ADDR).add(SUBSYSTEM, JMX);

        request.get(MBEAN_NAME).set(name.toString());
        request.get(MBEAN_ATTRIBUTE_NAME).set(attribute);

        ModelNode response = client.execute(request);
        if (!SUCCESS.equals(response.get(OUTCOME).asString())) {
            throw new IOException("Failed operation: " + response.get(FAILURE_DESCRIPTION).toString());
        }
        byte[] bytes = response.get(RESULT).asBytes();
        try {
            return Object.class.cast(new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject());
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    public AttributeList getAttributes(ObjectName name, String[] attributes) throws InstanceNotFoundException, ReflectionException, IOException {
        return null;
    }

    public void setAttribute(ObjectName name, Attribute attribute) throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException, IOException {
    }

    public AttributeList setAttributes(ObjectName name, AttributeList attributes) throws InstanceNotFoundException, ReflectionException, IOException {
        return null;
    }

    public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature) throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
        File notifications = File.createTempFile("arq", "notifications");
        FakeJMXNotificationThread thread = new FakeJMXNotificationThread(notifications, listeners);
        thread.start();

        ModelNode request = new ModelNode();
        request.get(OP).set(INVOKE_MBEAN_RAW);
        request.get(OP_ADDR).add(SUBSYSTEM, JMX);

        request.get(MBEAN_NAME).set(name.toString());
        request.get(MBEAN_OPERATION_NAME).set(operationName);
        request.get(NOTIFICATION_FILE).set(notifications.getAbsolutePath());

        if (signature != null) {
            for (String param : signature) {
                request.get(SIGNATURE).add(param);
            }
        }

        if (params != null) {
            for (Object param : params) {
                request.get(PARAMETERS).add(getBytes(param));
            }
        }

        ModelNode response = client.execute(request);
        thread.done();
        notifications.delete();
        if (!SUCCESS.equals(response.get(OUTCOME).asString())) {
            throw new IOException("Failed operation: " + response.get(FAILURE_DESCRIPTION).toString());
        }
        byte[] bytes = response.get(RESULT).asBytes();
        try {
            return new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    // Yuck!
    private byte[] getBytes(Object param) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(out);
        oos.writeObject(param);
        oos.flush();
        return out.toByteArray();
    }

    public String getDefaultDomain() throws IOException {
        return null;
    }

    public String[] getDomains() throws IOException {
        return new String[0];
    }

    public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, IOException {
        listeners.add(listener);
    }

    public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, IOException {
    }

    public void removeNotificationListener(ObjectName name, ObjectName listener) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
    }

    public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
    }

    public void removeNotificationListener(ObjectName name, NotificationListener listener) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        listeners.remove(listener);
    }

    public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter, Object handback) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
        listeners.remove(listener);
    }

    public MBeanInfo getMBeanInfo(ObjectName name) throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException {
        ModelNode request = new ModelNode();
        request.get(OP).set(GET_MBEAN_INFO_RAW);
        request.get(OP_ADDR).add(SUBSYSTEM, JMX);

        request.get(MBEAN_NAME).set(name.toString());

        ModelNode response = client.execute(request);
        if (!SUCCESS.equals(response.get(OUTCOME).asString())) {
            throw new IOException("Failed operation: " + response.get(FAILURE_DESCRIPTION).toString());
        }
        byte[] bytes = response.get(RESULT).asBytes();
        try {
            return MBeanInfo.class.cast(new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject());
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }

    public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException, IOException {
        return false;
    }

    public static class FakeJMXNotificationThread extends Thread {
        private final File file;
        private final List<NotificationListener> listeners;
        private final AtomicBoolean done = new AtomicBoolean(false);

        public FakeJMXNotificationThread(File file, List<NotificationListener> listeners) {
            this.file = file;
            this.listeners = listeners;
        }

        public void done() {
            done.set(true);
            this.interrupt();
        }

        @Override
        public void run() {
            PollingFileInputStream is;
            try {
                is = new PollingFileInputStream(new FileInputStream(file), done);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return;
            }

            ObjectInputStream ois;
            try {
                ois = new ObjectInputStream(is);
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    is.close();
                } catch (IOException e1) {
                }
                return;
            }


            while (!done.get()) {
                try {
                    Notification n = (Notification) ois.readObject();
                    for (NotificationListener listener : listeners) {
                        listener.handleNotification(n, null);
                    }
                } catch (Exception e) {
                }
            }

            try {
                is.close();
            } catch (IOException e) {
            }
        }
    }
}
