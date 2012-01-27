package org.jboss.as.jpa.container;

import java.io.Serializable;

import org.jboss.as.jpa.JpaMessages;

/**
 * Structure used to track SFSB references to an entity manager
 * <p/>
 * This class is not thread safe, it should not be accessed by multiple threads.
 * <p/>
 * Reference count is initially 1
 *
 * @author Stuart Douglas
 */
public class ReferenceCountedEntityManager implements Serializable {

    private static final long serialVersionUID = 456457893L;

    private final ExtendedEntityManager entityManager;
    private volatile int referenceCount = 1;

    public ReferenceCountedEntityManager(final ExtendedEntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public void increaseReferenceCount() {
        referenceCount++;
    }

    public void close() {
        if (--referenceCount == 0) {
            entityManager.containerClose();
        }
        // referenceCount should never be negative, if it is we need to fix the bug that caused it to decrement too much
        if (referenceCount < 0) {
            throw JpaMessages.MESSAGES.referenceCountedEntityManagerNegativeCount(referenceCount, entityManager.getScopedPuName());
        }
    }

    public ExtendedEntityManager getEntityManager() {
        return entityManager;
    }

    public int getReferenceCount() {
        return referenceCount;
    }
}
