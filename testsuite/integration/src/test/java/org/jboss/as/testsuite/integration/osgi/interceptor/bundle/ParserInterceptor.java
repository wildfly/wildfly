/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.testsuite.integration.osgi.interceptor.bundle;

import java.io.IOException;
import java.util.Properties;

import org.jboss.logging.Logger;
import org.jboss.osgi.deployment.interceptor.AbstractLifecycleInterceptor;
import org.jboss.osgi.deployment.interceptor.InvocationContext;
import org.jboss.osgi.deployment.interceptor.LifecycleInterceptorException;
import org.jboss.osgi.vfs.VirtualFile;
import org.osgi.framework.Bundle;

/**
 * An interceptor that creates and attaches HttpMetadata.
 * 
 * @author thomas.diesler@jboss.com
 * @since 23-Oct-2009
 */
public class ParserInterceptor extends AbstractLifecycleInterceptor {
    // Provide logging
    private static final Logger log = Logger.getLogger(ParserInterceptor.class);

    ParserInterceptor() {
        // Add the provided output
        addOutput(HttpMetadata.class);
    }

    public void invoke(int state, InvocationContext context) {
        // Do nothing if the metadata is already available
        HttpMetadata metadata = context.getAttachment(HttpMetadata.class);
        if (metadata != null)
            return;

        // Parse and create metadta on STARTING
        if (state == Bundle.STARTING) {
            try {
                VirtualFile root = context.getRoot();
                VirtualFile propsFile = root.getChild("/http-metadata.properties");
                if (propsFile != null) {
                    log.info("Create and attach HttpMetadata");
                    metadata = createHttpMetadata(propsFile);
                    context.addAttachment(HttpMetadata.class, metadata);
                }
            } catch (IOException ex) {
                throw new LifecycleInterceptorException("Cannot parse metadata", ex);
            }
        }
    }

    private HttpMetadata createHttpMetadata(VirtualFile propsFile) throws IOException {
        Properties props = new Properties();
        props.load(propsFile.openStream());

        HttpMetadata metadata = new HttpMetadata(props.getProperty("servlet.name"));
        return metadata;
    }
}
