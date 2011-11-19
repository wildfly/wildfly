/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * A HORRIBLE HORRIBLE HACK. Supports tunneling of JMX over management.
 * Notifications are mapped to file polling, since the management
 * protocol does not support a push model.
 *
 * @author Jason T. Greene
 */
public class InvokeMBeanRaw extends AbstractRuntimeOnlyHandler {

    private static final String NOTIFICATION_FILE = "notification-file";
    private static final String MBEAN_NAME = "mbean-name";
    private static final String MBEAN_OPERATION_NAME = "mbean-operation-name";
    private static final String SIGNATURE = "signature";
    private static final String PARAMETERS = "parameters";

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        ServiceController<?> service = context.getServiceRegistry(false).getRequiredService(MBeanServerService.SERVICE_NAME);
        MBeanServer server = (MBeanServer) service.getValue();

        String name = operation.require(MBEAN_NAME).asString();
        String operationName = operation.require(MBEAN_OPERATION_NAME).asString();

        ArrayList<String> signatureList = new ArrayList<String>();
        if (operation.hasDefined(SIGNATURE)) {
            for (ModelNode node : operation.get(SIGNATURE).asList()) {
                signatureList.add(node.asString());
            }
        }

        ArrayList<Object> parameterList = new ArrayList<Object>();
        if (operation.hasDefined(PARAMETERS)) {
            for (ModelNode node : operation.get(PARAMETERS).asList()) {
                parameterList.add(getObject(node.asBytes()));
            }
        }

        File file = operation.hasDefined(NOTIFICATION_FILE) ? new File(operation.get(NOTIFICATION_FILE).asString()) : null;

        Object result;
        FileNotificationListener listener = null;
        ObjectName beanName = null;
        try {
            beanName = ObjectName.getInstance(name);
            boolean notifications = server.isInstanceOf(beanName, NotificationBroadcaster.class.getName());

            if (notifications && file != null && file.exists()) {
                listener = new FileNotificationListener(file);
                server.addNotificationListener(beanName, listener, null, null);
            }
            result = server.invoke(beanName, operationName, parameterList.toArray(), signatureList.toArray(new String[0]));
        } catch (Exception e) {
            throw new OperationFailedException(e.getMessage(), e, new ModelNode().set(e.getMessage()));
        } finally {
            if (listener != null) {
                try {
                    server.removeNotificationListener(beanName, listener);
                    listener.done();
                } catch (Exception e) {
                }
            }
        }

        context.getResult().set(getBytes(result));
        context.completeStep();
    }

    private static Object getObject(byte[] bytes) throws OperationFailedException {
        try {
            return new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
        } catch (Exception e) {
            throw new OperationFailedException(new ModelNode().set(e.getMessage()));
        }
    }

     // Yuck!
     static byte[] getBytes(Object param) throws OperationFailedException {
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         ObjectOutputStream oos;
         try {
             oos = new ObjectOutputStream(out);
             oos.writeObject(param);
             oos.flush();
             return out.toByteArray();
         } catch (Exception e) {
            throw new OperationFailedException(new ModelNode().set(e.getMessage()));
        }
     }

    private class FileNotificationListener implements NotificationListener {
        private ObjectOutputStream oos;

        public FileNotificationListener(File file) {
            try {
                this.oos = new ObjectOutputStream(new FileOutputStream(file));
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public void handleNotification(Notification notification, Object handback) {
            try {
                oos.writeObject(notification);
                oos.flush();
            } catch (IOException e) {
            }
        }

        public void done() {
            try {
                oos.close();
            } catch (Exception e) {
            }
        }
    }
}
