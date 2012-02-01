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
package org.jboss.as.host.controller;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
public class JvmElementTestUtils {

    public static JvmElement create(JvmType type) {
        JvmElement element = new JvmElement("test");
        element.setJvmType(type);
        return element;
    }

    public static void setDebugEnabled(JvmElement element, boolean value) {
        element.setDebugEnabled(value);
    }

    public static void setDebugOptions(JvmElement element, String value) {
        element.setDebugOptions(value);
    }

    public static void setHeapSize(JvmElement element, String value) {
        element.setHeapSize(value);
    }

    public static void setMaxHeap(JvmElement element, String value) {
        element.setMaxHeap(value);
    }

    public static void setPermgenSize(JvmElement element, String value) {
        element.setPermgenSize(value);
    }

    public static void setMaxPermgen(JvmElement element, String value) {
        element.setMaxPermgen(value);
    }

    public static void setStack(JvmElement element, String value) {
        element.setStack(value);
    }

    public static void setAgentLib(JvmElement element, String value) {
        element.setAgentLib(value);
    }

    public static void setAgentPath(JvmElement element, String value) {
        element.setAgentPath(value);
    }

    public static void setJavaagent(JvmElement element, String value) {
        element.setJavaagent(value);
    }

    public static void addJvmOption(JvmElement element, String value) {
        element.getJvmOptions().addOption(value);
    }

}
