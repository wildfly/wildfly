package org.wildfly.clustering.web.infinispan.session;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.wildfly.clustering.web.infinispan.session.SimpleSessionMetaData;
import org.wildfly.clustering.web.infinispan.session.Time;
import org.wildfly.clustering.web.session.SessionMetaData;

import static org.junit.Assert.*;

public class SimpleSessionMetaDataTestCase {
    @Test
    public void isNew() {
        SessionMetaData metaData = new SimpleSessionMetaData();
        assertTrue(metaData.isNew());

        metaData.setLastAccessedTime(new Date());
        assertFalse(metaData.isNew());

        metaData = new SimpleSessionMetaData(new Date(), new Date(), new Time(0, TimeUnit.SECONDS));
        assertFalse(metaData.isNew());
    }

    @Test
    public void isExpired() {
        SessionMetaData metaData = new SimpleSessionMetaData();
        assertFalse(metaData.isExpired());

        Date now = new Date();

        metaData.setMaxInactiveInterval(1, TimeUnit.MINUTES);
        assertFalse(metaData.isExpired());

        metaData.setLastAccessedTime(new Date(now.getTime() - metaData.getMaxInactiveInterval(TimeUnit.MILLISECONDS) - 1));
        assertTrue(metaData.isExpired());
    }
}
