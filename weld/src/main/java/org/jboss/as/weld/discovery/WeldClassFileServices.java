/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.weld.discovery;

import java.util.concurrent.ExecutionException;

import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.weld.WeldMessages;
import org.jboss.weld.resources.spi.ClassFileInfo;
import org.jboss.weld.resources.spi.ClassFileServices;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 *
 * @author Martin Kouba
 */
public class WeldClassFileServices implements ClassFileServices {

    private CompositeIndex index;

    private LoadingCache<String, WeldClassFileInfo> weldClassInfoCache;

    private class WeldClassInfoLoader extends CacheLoader<String, WeldClassFileInfo> {
        @Override
        public WeldClassFileInfo load(String key) throws Exception {
            return new WeldClassFileInfo(key, index);
        }
    }

    /**
     *
     * @param index
     */
    public WeldClassFileServices(CompositeIndex index) {
        if (index == null) {
            throw WeldMessages.MESSAGES.cannotUseAtRuntime(ClassFileServices.class.getSimpleName());
        }
        this.index = index;
        this.weldClassInfoCache = CacheBuilder.newBuilder().build(new WeldClassInfoLoader());
    }

    @Override
    public ClassFileInfo getClassFileInfo(String className) {
        try {
            return weldClassInfoCache.get(className);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void cleanupAfterBoot() {
        this.index = null;
        this.weldClassInfoCache.invalidateAll();
        this.weldClassInfoCache = null;
    }

    @Override
    public void cleanup() {
        cleanupAfterBoot();
    }

}
