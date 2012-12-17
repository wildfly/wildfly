/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.core.model.bridge.local;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelController.OperationTransaction;
import org.jboss.as.core.model.bridge.impl.ClassLoaderObjectConverterImpl;
import org.jboss.as.core.model.bridge.shared.ObjectSerializer;
import org.jboss.dmr.ModelNode;

/**
 * This will run in the child classloader
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class OperationTransactionControlProxy implements ModelController.OperationTransactionControl{

    final ClassLoaderObjectConverter converter;
    final Object mainTransactionControl;


    public OperationTransactionControlProxy(Object mainTransactionControl) {
        this.converter = new ClassLoaderObjectConverterImpl(ObjectSerializer.class.getClassLoader(), this.getClass().getClassLoader());
        this.mainTransactionControl = mainTransactionControl;
    }


    @Override
    public void operationPrepared(OperationTransaction transaction, ModelNode result) {
        Class<?> mainClass = mainTransactionControl.getClass();
        ClassLoader mainCl = mainClass.getClassLoader();

        try {
            Method method = mainClass.getMethod("operationPrepared",
                    mainCl.loadClass(ModelController.OperationTransaction.class.getName()),
                    mainCl.loadClass(ModelNode.class.getName()));

            method.setAccessible(true);

            Class<?> mainOpTxClass = mainCl.loadClass(OperationTransactionProxy.class.getName());
            Constructor<?> ctor = mainOpTxClass.getConstructors()[0];
            Object mainOpTxProxy = ctor.newInstance(transaction);

            method.invoke(mainTransactionControl, mainOpTxProxy, convertModelNodeToMainCl(result));
        } catch (Exception e) {
            unwrapInvocationTargetRuntimeException(e);
            throw new RuntimeException(e);
        }
    }

    private Object convertModelNodeToMainCl(ModelNode modelNode) throws Exception {
        ObjectSerializer localSerializer = ObjectSerializer.FACTORY.createSerializer(this.getClass().getClassLoader());
        ObjectSerializer mainSerializer = ObjectSerializer.FACTORY.createSerializer(ObjectSerializer.class.getClassLoader());
        return mainSerializer.deserializeModelNode(localSerializer.serializeModelNode(modelNode));
    }

    private static void unwrapInvocationTargetRuntimeException(Exception e) {
        e.printStackTrace();
        if (e instanceof InvocationTargetException) {
            Throwable t = e.getCause();
            if (t instanceof RuntimeException) {
                throw (RuntimeException)t;
            }
        }
    }

    public static class OperationTransactionProxy implements ModelController.OperationTransaction {
        private final Object tx;

        public OperationTransactionProxy(Object tx) {
            this.tx = tx;
        }

        @Override
        public void commit() {
            invoke("commit");
        }

        @Override
        public void rollback() {
            invoke("rollback");
        }

        private void invoke(String name) {
            try {
                Method m = tx.getClass().getMethod(name);
                m.setAccessible(true);
                m.invoke(tx);
            } catch (Exception e) {
                unwrapInvocationTargetRuntimeException(e);
                throw new RuntimeException(e);
            }
        }
    }
}
