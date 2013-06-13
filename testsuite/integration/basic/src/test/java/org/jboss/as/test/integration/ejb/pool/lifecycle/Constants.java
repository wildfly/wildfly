package org.jboss.as.test.integration.ejb.pool.lifecycle;

/**
 * @author: Jaikiran Pai
 */
interface Constants {

    String QUEUE_JNDI_NAME = "queue/org.jboss.as.test.integration.ejb.pool.lifecycle.PooledEJBLifecycleTestCase-queue";
    String REPLY_MESSAGE_PREFIX = "org.jboss.as.test.integration.ejb.pool.lifecycle.PooledEJBLifecycleTestCase-queue-reply-prefix";
}
