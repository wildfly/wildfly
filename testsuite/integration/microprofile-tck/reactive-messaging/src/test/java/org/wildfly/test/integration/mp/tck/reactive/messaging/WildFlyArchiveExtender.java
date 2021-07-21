package org.wildfly.test.integration.mp.tck.reactive.messaging;

import org.eclipse.microprofile.reactive.messaging.tck.ArchiveExtender;
import org.eclipse.microprofile.reactive.messaging.tck.TckBase;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class WildFlyArchiveExtender implements ArchiveExtender {
    @Override
    public void extend(JavaArchive archive) {
        archive.addClass(TckBase.class);
    }
}
