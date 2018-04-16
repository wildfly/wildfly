/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.test.integration.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;
import org.wildfly.classchange.agent.FakereplaceWebsocketProtocol;

public final class TestProtocol extends FakereplaceWebsocketProtocol {

    private final Logger logger = Logger.getLogger(TestProtocol.class);

    private final Map<String, byte[]> changedSrcs = new ConcurrentHashMap<>();
    private final Map<String, byte[]> changedClasses = new ConcurrentHashMap<>();
    private final Map<String, byte[]> changedWebResources = new ConcurrentHashMap<>();

    public void addChangedSrc(String key, byte[] value) {
        changedSrcs.put(key, value);
    }

    public void addChangedClass(String key, byte[] value) {
        changedClasses.put(key, value);
    }

    public void addChangedWebResource(String key, byte[] value) {
        changedWebResources.put(key, value);
    }

    @Override
    protected Map<String, byte[]> changedSrcs() {
        Map<String, byte[]> ret = new HashMap<>(changedSrcs);
        changedSrcs.clear();
        return ret;
    }

    @Override
    protected Map<String, byte[]> changedClasses() {
        Map<String, byte[]> ret = new HashMap<>(changedClasses);
        changedClasses.clear();
        return ret;
    }

    @Override
    protected Map<String, byte[]> changedWebResources() {
        Map<String, byte[]> ret = new HashMap<>(changedWebResources);
        changedWebResources.clear();
        return ret;
    }

    @Override
    protected void logMessage(String s) {
        logger.error(s);
    }

    @Override
    protected void error(Throwable throwable) {
        throwable.printStackTrace();
    }

    @Override
    protected void done() {
    }
}
