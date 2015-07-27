/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ee.concurrent.function;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOW_RESOURCE_SERVICE_RESTART;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.lang.reflect.Field;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;

import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.ee.subsystem.EeExtension;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;

/**
 * 
 * @author Hynek Svabek
 *
 */
public class AbstractEEConcurrencyUtilitiesTestCase {

    protected static final PathAddress EE_SUBSYSTEM_PATH_ADDRESS = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, EeExtension.SUBSYSTEM_NAME));
    
    @ArquillianResource
    protected ManagementClient managementClient;

    public AbstractEEConcurrencyUtilitiesTestCase() {
        super();
    }
    
    protected void assertEquals(Object expected, Object actual) {
        assertEquals(null, expected, actual);
    }

    protected void assertEquals(String message, Object expected, Object actual) {
        if(expected instanceof Number && actual instanceof Number){
            expected = ((Number)expected).longValue();
            actual = ((Number)actual).longValue();
        }
        Assert.assertEquals(message, expected, actual);
    }

    protected void safeResourceRemove(PathAddress pathAddress, String jndiName)
            throws Exception {
                // remove
                final ModelNode removeResult = removeResource(pathAddress, jndiName);
                Assert.assertFalse(removeResult.get(FAILURE_DESCRIPTION).toString(), removeResult.get(FAILURE_DESCRIPTION)
                        .isDefined());
                try {
                    new InitialContext().lookup(jndiName);
                    Assert.fail();
                } catch (NameNotFoundException e) {
                    // expected
                }
            }

    protected ModelNode removeResource(PathAddress pathAddress, String jndiName) throws Exception {
        final ModelNode removeOperation = Util.createRemoveOperation(pathAddress);
        removeOperation.get(OPERATION_HEADERS, ALLOW_RESOURCE_SERVICE_RESTART).set(true);
        return managementClient.getControllerClient().execute(removeOperation);
    }

    public static <T> T getValue(Class<T> clazz, Object object, String fieldName) {
        return getValueFromField(clazz, getDeclaredField(object, fieldName), object);
    }
    
    public static <T> T getValueFromField(Class<T> clazz, Field field,
            Object object) {
                if(field == null){
                    return null;
                }
                
                Object value;
                try {
                    field.setAccessible(true);
                    value = field.get(object);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    value = null;
                }finally{
                    field.setAccessible(false);
                }
                
                return clazz.cast(value);
            }

    public static Field getDeclaredField(Object object, String fieldName) {
        return getDeclaredField(object.getClass(), object, fieldName);
    }

    public static Field getDeclaredField(Class<?> clazz, Object object, String fieldName) {
        Field declaredField = null;
        try {
            declaredField = clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException | SecurityException e) {
            if(clazz.getSuperclass() != null){
                declaredField = getDeclaredField(clazz.getSuperclass(), object, fieldName);
            }
        }
    
        return declaredField;
    }


}