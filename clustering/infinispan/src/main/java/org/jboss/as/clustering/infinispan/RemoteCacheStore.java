/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan;

import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.infinispan.loaders.CacheLoaderMetadata;

/**
 * Workaround for ISPN-1831
 * @author Paul Ferraro
 */
@CacheLoaderMetadata(configurationClass = RemoteCacheStore.RemoteCacheStoreConfig.class)
public class RemoteCacheStore extends org.infinispan.loaders.remote.RemoteCacheStore {

    public static class RemoteCacheStoreConfig extends org.infinispan.loaders.remote.RemoteCacheStoreConfig {
        private static final long serialVersionUID = -412497426413791208L;

        public RemoteCacheStoreConfig() {
            this.setCacheLoaderClassName(RemoteCacheStore.class.getName());
        }

        public String getServerList() {
            return this.getHotRodClientProperties().getProperty(ConfigurationProperties.SERVER_LIST);
        }

        public void setServerList(String serverList) {
            this.getHotRodClientProperties().setProperty(ConfigurationProperties.SERVER_LIST, serverList);
        }

        public String getSoTimeout() {
            return this.getHotRodClientProperties().getProperty(ConfigurationProperties.SO_TIMEOUT);
        }

        public void setSoTimeout(String soTimeout) {
            this.getHotRodClientProperties().setProperty(ConfigurationProperties.SO_TIMEOUT, soTimeout);
        }

        public String getTcpNoDelay() {
            return this.getHotRodClientProperties().getProperty(ConfigurationProperties.TCP_NO_DELAY);
        }

        public void setTcpNoDelay(String tcpNoDelay) {
            this.getHotRodClientProperties().setProperty(ConfigurationProperties.TCP_NO_DELAY, tcpNoDelay);
        }
    }
}
