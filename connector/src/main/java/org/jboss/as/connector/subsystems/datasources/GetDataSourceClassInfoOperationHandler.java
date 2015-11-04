/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

/**
 *
 */
package org.jboss.as.connector.subsystems.datasources;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.services.driver.InstalledDriver;
import org.jboss.as.connector.services.driver.registry.DriverRegistry;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.ServiceController;

/**
 * Reads data-source and xa-data-source class information for a jdbc-driver.
 *
 * @author <a href="mailto:lgao@redhat.com">Lin Gao</a>
 */
public class GetDataSourceClassInfoOperationHandler implements OperationStepHandler {

    public static final GetDataSourceClassInfoOperationHandler INSTANCE = new GetDataSourceClassInfoOperationHandler();

    private GetDataSourceClassInfoOperationHandler() {
    }

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

        final String driverName = context.getCurrentAddressValue();
        if (context.isNormalServer()) {
            context.addStep(new OperationStepHandler() {

                @Override
                public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

                    ServiceController<?> sc = context.getServiceRegistry(false).getRequiredService(
                            ConnectorServices.JDBC_DRIVER_REGISTRY_SERVICE);
                    DriverRegistry driverRegistry = DriverRegistry.class.cast(sc.getValue());
                    ModelNode result = new ModelNode();
                    InstalledDriver driver = driverRegistry.getInstalledDriver(driverName);
                    String dataSourceClsName = driver.getDataSourceClassName();
                    String xaDataSourceClsName = driver.getXaDataSourceClassName();
                    if (dataSourceClsName != null) {
                        ModelNode dsNode = new ModelNode();
                        dsNode.get(dataSourceClsName).set(
                                mapToModelNode(findPropsFromCls(driver.getModuleName(), dataSourceClsName)));
                        result.add(dsNode);
                    }
                    if (xaDataSourceClsName != null) {
                        ModelNode xaDsNode = new ModelNode();
                        xaDsNode.get(xaDataSourceClsName).set(
                                mapToModelNode(findPropsFromCls(driver.getModuleName(), xaDataSourceClsName)));
                        result.add(xaDsNode);
                    }

                    context.getResult().set(result);
                }

            }, OperationContext.Stage.RUNTIME);
        }
    }

    private ModelNode mapToModelNode(Map<String, Type> props) {
        final ModelNode result = new ModelNode();
        props.forEach((k, v) -> {
            result.get(k).set(v.getTypeName());
        });
        return result;
    }

    private Map<String, Type> findPropsFromCls(ModuleIdentifier mid, String clsName) throws OperationFailedException {
        Class<?> cls = null;
        ClassLoader cl = null;
        if (mid != null) {
            try {
                cl = Module.getCallerModuleLoader().loadModule(mid).getClassLoader();
            } catch (ModuleLoadException e) {
                throw new OperationFailedException(ConnectorLogger.SUBSYSTEM_DATASOURCES_LOGGER.failedToLoadModuleDriver(mid
                        .toString()), e);
            }
        }
        if (cl == null) {
            cl = getClassLoader(this.getClass());
        }
        try {
            cls = Class.forName(clsName, true, cl);
        } catch (ClassNotFoundException e) {
        }
        if (cls == null) {
            try {
                cls = Class.forName(clsName, true, getClassLoader(this.getClass()));
            } catch (ClassNotFoundException e) {
            }
        }
        if (cls == null) {
            throw ConnectorLogger.SUBSYSTEM_DATASOURCES_LOGGER.failedToLoadClass(clsName);
        }
        Map<String, Type> result = new HashMap<>();
        for (Method method : getDeclaredMethods(cls)) {
            String mtdName = method.getName();
            if (Modifier.isPublic(method.getModifiers())
                    && (method.getReturnType().equals(void.class) || method.getReturnType().equals(Void.class))
                    && mtdName.startsWith("set") && method.getParameterCount() == 1
                    && isTypeMatched(method.getParameterTypes()[0])) {
                result.put(capitalize(mtdName.substring(3)), method.getParameterTypes()[0]);
            }
        }
        return result;
    }

    private boolean isTypeMatched(Class<?> clz) {
        if (clz.equals(String.class)) {
            return true;
        } else if (clz.equals(byte.class) || clz.equals(Byte.class)) {
            return true;
        } else if (clz.equals(short.class) || clz.equals(Short.class)) {
            return true;
        } else if (clz.equals(int.class) || clz.equals(Integer.class)) {
            return true;
        } else if (clz.equals(long.class) || clz.equals(Long.class)) {
            return true;
        } else if (clz.equals(float.class) || clz.equals(Float.class)) {
            return true;
        } else if (clz.equals(double.class) || clz.equals(Double.class)) {
            return true;
        } else if (clz.equals(boolean.class) || clz.equals(Boolean.class)) {
            return true;
        } else if (clz.equals(char.class) || clz.equals(Character.class)) {
            return true;
        } else if (clz.equals(InetAddress.class)) {
            return true;
        } else if (clz.equals(Class.class)) {
            return true;
        } else if (clz.equals(Properties.class)) {
            return true;
        }
        return false;
    }

    private String capitalize(String str) {
        if (str.length() == 1) {
            return str.toUpperCase(Locale.ENGLISH);
        }
        return str.substring(0, 1).toUpperCase(Locale.ENGLISH) + str.substring(1);
    }

    private static ClassLoader getClassLoader(final Class<?> clz) {
        if (System.getSecurityManager() == null)
            return clz.getClassLoader();
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            public ClassLoader run() {
                return clz.getClassLoader();
            }
        });
    }

    /**
     * Get the declared methods
     *
     * @param c The class
     * @return The methods
     */
    private static Method[] getDeclaredMethods(final Class<?> c) {
        if (System.getSecurityManager() == null)
            return c.getDeclaredMethods();

        return AccessController.doPrivileged(new PrivilegedAction<Method[]>() {
            public Method[] run() {
                return c.getDeclaredMethods();
            }
        });
    }

}
