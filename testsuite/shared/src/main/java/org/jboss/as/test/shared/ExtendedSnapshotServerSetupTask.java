package org.jboss.as.test.shared;

import org.jboss.as.arquillian.setup.SnapshotServerSetupTask;

public class ExtendedSnapshotServerSetupTask extends SnapshotServerSetupTask {

    @Override
    protected long timeout() {
        return 20L;
    }
}
