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
    //The defaults for these come from XnioWorker

    /*static final OptionAttributeDefinition THREAD_DAEMON = new OptionAttributeDefinition.Builder(Constants.THREAD_DAEMON, Options.THREAD_DAEMON)
            .setDefaultValue(new ModelNode(false))
            .build();*/
    /*static final OptionAttributeDefinition WORKER_TASK_CORE_THREADS = new OptionAttributeDefinition.Builder(Constants.WORKER_TASK_CORE_THREADS, Options.WORKER_TASK_CORE_THREADS)
            .setDefaultValue(new ModelNode(60))
            .build();
    */
    static final OptionAttributeDefinition WORKER_TASK_MAX_THREADS = new OptionAttributeDefinition.Builder(Constants.WORKER_TASK_MAX_THREADS, Options.WORKER_TASK_MAX_THREADS)
            .setDefaultValue(new ModelNode(60))
            .build();
    static final OptionAttributeDefinition WORKER_TASK_KEEPALIVE = new OptionAttributeDefinition.Builder(Constants.WORKER_TASK_KEEPALIVE, Options.WORKER_TASK_KEEPALIVE)
            .setDefaultValue(new ModelNode(60))
            .build();
    static final OptionAttributeDefinition STACK_SIZE = new OptionAttributeDefinition.Builder(Constants.STACK_SIZE, Options.STACK_SIZE)
            .setDefaultValue(new ModelNode(0L))
            .build();
    static final OptionAttributeDefinition WORKER_IO_THREADS = new OptionAttributeDefinition.Builder(Constants.WORKER_IO_THREADS, Options.WORKER_IO_THREADS)
            .setDefaultValue(new ModelNode(4))
            .build();
    /*static final OptionAttributeDefinition WORKER_TASK_LIMIT = new OptionAttributeDefinition.Builder(Constants.WORKER_TASK_LIMIT, Options.WORKER_TASK_LIMIT)
            .setDefaultValue(new ModelNode(0x4000))
            .build();*/

    /*
    workers support...
    WORKER_NAME THREAD_DAEMON WORKER_TASK_CORE_THREADS WORKER_TASK_MAX_THREADS WORKER_TASK_KEEPALIVE STACK_SIZE WORKER_READ_THREADS WORKER_WRITE_THREADS
    WORKER_NAME should be derived from the resource name for ease of debugging and whatnot
    maybe something like "%s I/O" where %s is the name of the resource
    in current upstream, WORKER_TASK_CORE_THREADS and WORKER_TASK_KEEPALIVE have no effect
    or WORKER_TASK_LIMIT which should also be supported
    actually, forget that last one
    WORKER_TASK_LIMIT should diaf
    limiting the work queue will just lead to 500 errors and other problems
     */


    static OptionAttributeDefinition[] ATTRIBUTES = new OptionAttributeDefinition[]{
            WORKER_IO_THREADS,
            //WORKER_TASK_CORE_THREADS,
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
