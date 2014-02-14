/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.wildfly.extension.io;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.dmr.ModelNode;
import org.xnio.Options;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
class WorkerResourceDefinition extends PersistentResourceDefinition {

    static final OptionAttributeDefinition WORKER_TASK_CORE_THREADS = new OptionAttributeDefinition.Builder(Constants.WORKER_TASK_CORE_THREADS, Options.WORKER_TASK_CORE_THREADS)
            .setDefaultValue(new ModelNode(2))
            .build();
    static final OptionAttributeDefinition WORKER_TASK_MAX_THREADS = new OptionAttributeDefinition.Builder(Constants.WORKER_TASK_MAX_THREADS, Options.WORKER_TASK_MAX_THREADS)
            .build();
    static final OptionAttributeDefinition WORKER_TASK_KEEPALIVE = new OptionAttributeDefinition.Builder(Constants.WORKER_TASK_KEEPALIVE, Options.WORKER_TASK_KEEPALIVE)
            .setDefaultValue(new ModelNode(60))
            .build();
    static final OptionAttributeDefinition STACK_SIZE = new OptionAttributeDefinition.Builder(Constants.STACK_SIZE, Options.STACK_SIZE)
            .setDefaultValue(new ModelNode(0L))
            .build();
    static final OptionAttributeDefinition WORKER_IO_THREADS = new OptionAttributeDefinition.Builder(Constants.WORKER_IO_THREADS, Options.WORKER_IO_THREADS)
            .build();
    /*static final OptionAttributeDefinition WORKER_TASK_LIMIT = new OptionAttributeDefinition.Builder(Constants.WORKER_TASK_LIMIT, Options.WORKER_TASK_LIMIT)
            .setDefaultValue(new ModelNode(0x4000))
            .build();*/

    static OptionAttributeDefinition[] ATTRIBUTES = new OptionAttributeDefinition[]{
            WORKER_IO_THREADS,
            WORKER_TASK_CORE_THREADS,
            WORKER_TASK_KEEPALIVE,
            WORKER_TASK_MAX_THREADS,
            STACK_SIZE
    };

    static final Map<String, OptionAttributeDefinition> ATTRIBUTES_BY_XMLNAME;

    static {
        Map<String, OptionAttributeDefinition> attrs = new HashMap<>();
        for (AttributeDefinition attr : ATTRIBUTES) {
            attrs.put(attr.getXmlName(), (OptionAttributeDefinition) attr);
        }
        ATTRIBUTES_BY_XMLNAME = Collections.unmodifiableMap(attrs);
    }


    static final WorkerResourceDefinition INSTANCE = new WorkerResourceDefinition();


    private WorkerResourceDefinition() {
        super(IOExtension.WORKER_PATH,
                IOExtension.getResolver(Constants.WORKER),
                WorkerAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return (Collection) ATTRIBUTES_BY_XMLNAME.values();
    }
}
