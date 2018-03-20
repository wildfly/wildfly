/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.subsystems.datasources;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.services.driver.InstalledDriver;
import org.jboss.as.connector.services.driver.registry.DriverRegistry;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.server.Services;
import org.jboss.as.server.moduleservice.ServiceModuleLoader;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Reads data-source and xa-data-source class information for a jdbc-driver.
 *
 * @author <a href="mailto:lgao@redhat.com">Lin Gao</a>
 */
@SuppressWarnings("deprecation")
// ModuleIdentifier is used in InstalledDriver, which is deprecated.
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

                    ServiceRegistry registry = context.getServiceRegistry(false);
                    DriverRegistry driverRegistry = (DriverRegistry)registry.getRequiredService(ConnectorServices.JDBC_DRIVER_REGISTRY_SERVICE).getValue();
                    ServiceModuleLoader serviceModuleLoader = (ServiceModuleLoader)registry.getRequiredService(Services.JBOSS_SERVICE_MODULE_LOADER).getValue();
                    InstalledDriver driver = driverRegistry.getInstalledDriver(driverName);
                    if (driver == null) {
                        context.getResult().set(new ModelNode());
                        return;
                    }
                    ModelNode result = dsClsInfoNode(serviceModuleLoader, driver.getModuleName(), driver.getDataSourceClassName(),
                            driver.getXaDataSourceClassName());
                    context.getResult().set(result);
                }

            }, OperationContext.Stage.RUNTIME);
        }
    }

    static ModelNode dsClsInfoNode(ServiceModuleLoader serviceModuleLoader, ModuleIdentifier mid, String dsClsName, String xaDSClsName)
            throws OperationFailedException {
        ModelNode result = new ModelNode();
        if (dsClsName != null) {
            ModelNode dsNode = new ModelNode();
            dsNode.get(dsClsName).set(findPropsFromCls(serviceModuleLoader, mid, dsClsName));
            result.add(dsNode);
        }
        if (xaDSClsName != null) {
            ModelNode xaDSNode = new ModelNode();
            xaDSNode.get(xaDSClsName).set(findPropsFromCls(serviceModuleLoader, mid, xaDSClsName));
            result.add(xaDSNode);
        }
        return result;
    }

    private static ModelNode findPropsFromCls(ServiceModuleLoader serviceModuleLoader, ModuleIdentifier mid, String clsName) throws OperationFailedException {
        Class<?> cls = null;
        if (mid != null) {
            try {
                cls = Class.forName(clsName, true, serviceModuleLoader.loadModule(mid.toString()).getClassLoader());
            } catch (ModuleLoadException | ClassNotFoundException e) {
                throw ConnectorLogger.SUBSYSTEM_DATASOURCES_LOGGER.failedToLoadDataSourceClass(clsName, e);
            }
        }
        if (cls == null) {
            try {
                cls = Class.forName(clsName);
            } catch (ClassNotFoundException e) {
                throw ConnectorLogger.SUBSYSTEM_DATASOURCES_LOGGER.failedToLoadDataSourceClass(clsName, e);
            }
        }
        Map<String, Type> methodsMap = new TreeMap<>();
        for (Method method : possiblePropsSetters(cls)) {
            methodsMap.putIfAbsent(deCapitalize(method.getName().substring(3)), method.getParameterTypes()[0]);
        }
        final ModelNode result = new ModelNode();
        for (Map.Entry<String, Type> prop: methodsMap.entrySet()) {
            result.get(prop.getKey()).set(prop.getValue().getTypeName());
        }
        return result;
    }

    /**
     * Check whether the types that JCA Injection knows.
     *
     * @see Injection.findMethod()
     * @param clz the class
     * @return whether it is understandable
     */
    private static boolean isTypeMatched(Class<?> clz) {
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

    private static String deCapitalize(String str) {
        if (str.length() == 1) {
            return str.toLowerCase(Locale.US);
        }
        if (str.equals(str.toUpperCase(Locale.US))) { // all uppercase, just return
            return str;
        }
        return str.substring(0, 1).toLowerCase(Locale.US) + str.substring(1);
    }

    private static List<Method> possiblePropsSetters(Class<?> clz) {
        List<Method> hits = new ArrayList<>();
        while (!clz.equals(Object.class)) {
            Method[] methods = clz.getMethods();
            for (int i = 0; i < methods.length; i++) {
                final Method method = methods[i];
                if (!Modifier.isStatic(method.getModifiers())
                        && method.getName().length() > 3
                        && method.getName().startsWith("set")
                        && method.getParameterCount() == 1
                        && isTypeMatched(method.getParameterTypes()[0]))
                    hits.add(method);
            }
            clz = clz.getSuperclass();
        }
        return hits;
    }
}
