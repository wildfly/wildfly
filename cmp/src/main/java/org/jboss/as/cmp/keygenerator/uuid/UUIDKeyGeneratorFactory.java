/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.cmp.keygenerator.uuid;

import org.jboss.as.cmp.keygenerator.KeyGenerator;
import org.jboss.as.cmp.keygenerator.KeyGeneratorFactory;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * This is the factory for UUID key generator
 *
 * @author <a href="mailto:loubyansky@ukr.net">Alex Loubyansky</a>
 * @version $Revision: 81030 $
 */
public class UUIDKeyGeneratorFactory implements KeyGeneratorFactory, Service<KeyGeneratorFactory> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("cmp", "keygen", UUIDKeyGeneratorFactory.class.getSimpleName());

    public void start(StartContext context) throws StartException {
    }

    public void stop(StopContext context) {
    }

    public KeyGeneratorFactory getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    /**
     * Returns a newly constructed key generator
     */
    public KeyGenerator getKeyGenerator() throws Exception {
        return new UUIDKeyGenerator();
    }
}
