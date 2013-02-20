package org.jboss.as.undertow;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimplePersistentResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.xnio.Options;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class WorkerResourceDefinition extends SimplePersistentResourceDefinition {


    //The defaults for these come from XnioWorker

    static final OptionAttributeDefinition THREAD_DAEMON = new OptionAttributeDefinition.Builder(Constants.THREAD_DAEMON, Options.THREAD_DAEMON)
            .setDefaultValue(new ModelNode(false))
            .build();
    static final OptionAttributeDefinition WORKER_TASK_CORE_THREADS = new OptionAttributeDefinition.Builder(Constants.WORKER_TASK_CORE_THREADS, Options.WORKER_TASK_CORE_THREADS)
            .setDefaultValue(new ModelNode(4))
            .build();
    static final OptionAttributeDefinition WORKER_TASK_MAX_THREADS = new OptionAttributeDefinition.Builder(Constants.WORKER_TASK_MAX_THREADS, Options.WORKER_TASK_MAX_THREADS)
            .setDefaultValue(new ModelNode(16))
            .build();
    static final OptionAttributeDefinition WORKER_TASK_KEEPALIVE = new OptionAttributeDefinition.Builder(Constants.WORKER_TASK_KEEPALIVE, Options.WORKER_TASK_KEEPALIVE)
            .setDefaultValue(new ModelNode(60))
            .build();
    static final OptionAttributeDefinition STACK_SIZE = new OptionAttributeDefinition.Builder(Constants.STACK_SIZE, Options.STACK_SIZE)
            .setDefaultValue(new ModelNode(10L))
            .build();
    static final OptionAttributeDefinition WORKER_IO_THREADS = new OptionAttributeDefinition.Builder(Constants.WORKER_IO_THREADS, Options.WORKER_IO_THREADS)
            .setDefaultValue(new ModelNode(1))
            .build();
    static final OptionAttributeDefinition WORKER_TASK_LIMIT = new OptionAttributeDefinition.Builder(Constants.WORKER_TASK_LIMIT, Options.WORKER_TASK_LIMIT)
            .setDefaultValue(new ModelNode(0x4000))
            .build();

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
            WORKER_TASK_CORE_THREADS,
            WORKER_TASK_KEEPALIVE,
            WORKER_TASK_LIMIT,
            WORKER_TASK_MAX_THREADS,
            THREAD_DAEMON,
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


    public static final WorkerResourceDefinition INSTANCE = new WorkerResourceDefinition();


    private WorkerResourceDefinition() {
        super(UndertowExtension.WORKER_PATH,
                UndertowExtension.getResolver(Constants.WORKER),
                WorkerAdd.INSTANCE,
                ReloadRequiredRemoveStepHandler.INSTANCE
        );
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        for (AttributeDefinition attr : WorkerResourceDefinition.ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
        }
    }

    @Override
    public AttributeDefinition[] getAttributes() {
        return ATTRIBUTES;
    }
}
