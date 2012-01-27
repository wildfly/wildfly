package org.jboss.as.jpa.container;

import java.io.Serializable;

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
    private int referenceCount = 1;

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
    }

    public ExtendedEntityManager getEntityManager() {
        return entityManager;
    }
}
