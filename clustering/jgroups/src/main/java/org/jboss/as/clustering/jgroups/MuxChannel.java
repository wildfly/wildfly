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
package org.jboss.as.clustering.jgroups;

import org.jgroups.Event;
import org.jgroups.JChannel;
import org.jgroups.UpHandler;
import org.jgroups.blocks.mux.MuxUpHandler;
import org.jgroups.blocks.mux.Muxer;
import org.jgroups.conf.ProtocolStackConfigurator;

/**
 * A JGroups channel that uses a MuxUpHandler by default.
 * @author Paul Ferraro
 */
public class MuxChannel extends JChannel {
    public MuxChannel(ProtocolStackConfigurator configurator) throws Exception {
        super(configurator);
        this.setUpHandler(new ClassLoaderAwareMuxUpHandler());
        this.setAddressGenerator(new TopologyAddressGenerator(this));
    }

    @Override
    public void setUpHandler(UpHandler handler) {
        UpHandler existingHandler = this.getUpHandler();
        if ((existingHandler != null) && (existingHandler instanceof Muxer)) {
            @SuppressWarnings("unchecked")
            Muxer<UpHandler> muxer = (Muxer<UpHandler>) existingHandler;
            muxer.setDefaultHandler(handler);
        } else {
            super.setUpHandler(handler);
        }
    }

    /**
     * Custom muxing up handler that decorates registered up handlers with class loader awareness.
     */
    private static class ClassLoaderAwareMuxUpHandler extends MuxUpHandler {
        @Override
        public void add(short id, UpHandler handler) {
            super.add(id, this.getUpHandler(handler));
        }

        @Override
        public Object up(Event event) {
            return super.up(event);
        }

        @Override
        public void setDefaultHandler(UpHandler handler) {
            super.setDefaultHandler(this.getUpHandler(handler));
        }

        private UpHandler getUpHandler(UpHandler handler) {
            return (handler instanceof ClassLoaderAwareUpHandler) ? handler : new ClassLoaderAwareUpHandler(handler);
        }
    }
}