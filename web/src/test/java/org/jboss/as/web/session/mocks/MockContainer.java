/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.web.session.mocks;

import org.apache.catalina.Loader;
import org.apache.catalina.Pipeline;
import org.apache.catalina.core.ContainerBase;
import org.apache.catalina.core.StandardPipeline;

/**
 * Mock Container impl to wrap a JBossCacheManager in unit tests.
 * 
 * @author <a href="brian.stansberry@jboss.com">Brian Stansberry</a>
 * @version $Revision: 82920 $
 */
public class MockContainer extends ContainerBase {

    private Loader loader;

    private Pipeline pipeline;

    @Override
    public Loader getLoader() {
        if (loader == null) {
            loader = new MockLoader();
            loader.setContainer(this);
        }
        return loader;
    }

    public Pipeline getPipeline() {
        if (pipeline == null) {
            pipeline = new StandardPipeline(this);
        }
        return pipeline;
    }

    public void setLoader(Loader loader) {
        this.loader = loader;
    }
}
