package org.wildfly.clustering.ejb.infinispan;/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.time.Duration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit test for ExpirationTracker class.
 */
public class ExpirationTrackerTestCase {

    @Test
    public void emptyTracker() {
        ExpirationTracker<String> expirationTracker = new ExpirationTracker<>(
                Duration.ofMillis(1));
        assertNull(expirationTracker.getExpiredId(System.currentTimeMillis()));
        expirationTracker.invalidateExpiration("non existent");
        expirationTracker.forget("non existent");
        assertEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertTrue(expirationTracker.getTrackedIds().isEmpty());
        expirationTracker.retryExpiration("non existent");
    }

    @Test
    public void singleElementTracker() throws InterruptedException {
        // create element tracker with single element
        ExpirationTracker<String> expirationTracker = new ExpirationTracker<>(
                Duration.ofMillis(1));
        expirationTracker.trackExpiration("id");

        // expire id
        assertNull(expirationTracker.getExpiredId(System.currentTimeMillis() - 100));
        assertEquals("id", expirationTracker.getExpiredId(System.currentTimeMillis() + 1));
        assertNull(expirationTracker.getExpiredId(System.currentTimeMillis()));

        // reestablish previous state of single element in tracker
        expirationTracker.trackExpiration("id2");

        // test expiration with get next expiration in millis
        Thread.sleep(1);
        long time = System.currentTimeMillis();
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id2"));
        assertTrue(expirationTracker.getNextExpirationInMillis() <= time);
        assertEquals("id2", expirationTracker.getExpiredId(System.currentTimeMillis()));
        assertEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertNull(expirationTracker.getExpiredId(System.currentTimeMillis()));
        assertTrue(expirationTracker.getTrackedIds().isEmpty());

        // reestablish previous state of single element in tracker
        expirationTracker.trackExpiration("id3");

        // test invalidate expiration
        Thread.sleep(1);
        time = System.currentTimeMillis();
        expirationTracker.invalidateExpiration("non existent");
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        expirationTracker.invalidateExpiration("id3");
        assertEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertNull(expirationTracker.getExpiredId(time));
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id3"));

        // test reescheduling the same id after invalidated expiration
        expirationTracker.trackExpiration("id3");
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id3"));
        Thread.sleep(1);
        time = System.currentTimeMillis();
        assertTrue(expirationTracker.getNextExpirationInMillis() <= time);
        assertEquals(expirationTracker.getNextExpirationInMillis(), expirationTracker.getNextExpirationInMillis());
        assertEquals("id3", expirationTracker.getExpiredId(time));
        assertEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertNull(expirationTracker.getExpiredId(time));
        assertTrue(expirationTracker.getTrackedIds().isEmpty());

        // reestablish previous state of single element in tracker
        expirationTracker.trackExpiration("id4");

        // test forgetting expiration
        expirationTracker.forget("id4");
        assertTrue(expirationTracker.getTrackedIds().isEmpty());
        assertEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertNull(expirationTracker.getExpiredId(time));

        // reestablish previous state of single element in tracker
        expirationTracker.trackExpiration("id5");

        // test invalidate expiration again
        Thread.sleep(1);
        time = System.currentTimeMillis();
        expirationTracker.invalidateExpiration("non existent");
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        expirationTracker.invalidateExpiration("id5");
        assertEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertNull(expirationTracker.getExpiredId(time));
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id5"));

        // test forgetting the id after invalidated expiration
        expirationTracker.forget("id5");
        assertTrue(expirationTracker.getTrackedIds().isEmpty());
        assertEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertNull(expirationTracker.getExpiredId(time));

        // scramble order in the collection with multiple track expiration calls
        // in practice order doesnt change because there is only one single element
        expirationTracker.trackExpiration("id6");
        Thread.sleep(3);
        expirationTracker.trackExpiration("id6");
        Thread.sleep(1);
        expirationTracker.trackExpiration("id6");
        expirationTracker.trackExpiration("id6");
        Thread.sleep(5);
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id6"));
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertEquals("id6", expirationTracker.getExpiredId(System.currentTimeMillis()));
    }

    @Test
    public void twoApartElementTrackerEditAlwaysTheFirstToExpire() throws InterruptedException {
        // create element tracker with single element
        ExpirationTracker<String> expirationTracker = new ExpirationTracker<>(
                Duration.ofMillis(1));
        expirationTracker.trackExpiration("id1");
        Thread.sleep(5);
        expirationTracker.trackExpiration("id2");

        // expire id
        assertNull(expirationTracker.getExpiredId(System.currentTimeMillis() - 100));
        assertEquals("id1", expirationTracker.getExpiredId(System.currentTimeMillis() + 1));
        assertNull(expirationTracker.getExpiredId(System.currentTimeMillis() - 100));
        assertEquals("id2", expirationTracker.getExpiredId(System.currentTimeMillis() + 1));
        assertNull(expirationTracker.getExpiredId(System.currentTimeMillis()));

        // reestablish previous state of two elements in tracker
        expirationTracker.trackExpiration("id3");
        Thread.sleep(5);
        expirationTracker.trackExpiration("id4");

        // test expiration with get next expiration in millis
        Thread.sleep(1);
        long time = System.currentTimeMillis();
        assertEquals(2, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id3"));
        assertTrue(expirationTracker.getTrackedIds().contains("id4"));
        assertTrue(expirationTracker.getNextExpirationInMillis() <= time);
        assertEquals("id3", expirationTracker.getExpiredId(System.currentTimeMillis()));
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id4"));
        assertEquals("id4", expirationTracker.getExpiredId(System.currentTimeMillis()));
        assertEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertNull(expirationTracker.getExpiredId(System.currentTimeMillis()));
        assertTrue(expirationTracker.getTrackedIds().isEmpty());

        // reestablish previous state of two elements in tracker
        expirationTracker.trackExpiration("id5");
        Thread.sleep(5);
        expirationTracker.trackExpiration("id6");

        // test invalidate expiration
        Thread.sleep(1);
        time = System.currentTimeMillis();
        expirationTracker.invalidateExpiration("non existent");
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        expirationTracker.invalidateExpiration("id5");
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertEquals("id6", expirationTracker.getExpiredId(time));
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id5"));

        // test rescheduling the same id after invalidated expiration
        expirationTracker.trackExpiration("id5");
        // again reestabilish the 2 element state in tracker
        expirationTracker.trackExpiration("id6");
        assertEquals(2, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id5"));
        assertTrue(expirationTracker.getTrackedIds().contains("id6"));
        Thread.sleep(1);
        time = System.currentTimeMillis();
        assertTrue(expirationTracker.getNextExpirationInMillis() <= time);
        assertEquals(expirationTracker.getNextExpirationInMillis(), expirationTracker.getNextExpirationInMillis());
        assertEquals("id5", expirationTracker.getExpiredId(time));
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id6"));
        assertEquals("id6", expirationTracker.getExpiredId(time));
        assertEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertNull(expirationTracker.getExpiredId(time));
        assertTrue(expirationTracker.getTrackedIds().isEmpty());

        // reestablish previous state of two elements in tracker
        expirationTracker.trackExpiration("id7");
        Thread.sleep(5);
        expirationTracker.trackExpiration("id8");

        // test forgetting expiration
        expirationTracker.forget("id7");
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id8"));
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertEquals("id8", expirationTracker.getExpiredId(time + 100));

        // reestablish previous state of two elements in tracker
        expirationTracker.trackExpiration("id9");
        Thread.sleep(5);
        expirationTracker.trackExpiration("id10");


        // test invalidate expiration again
        Thread.sleep(1);
        time = System.currentTimeMillis();
        expirationTracker.invalidateExpiration("non existent");
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        expirationTracker.invalidateExpiration("id9");
        assertTrue(expirationTracker.getNextExpirationInMillis() <= time);
        assertEquals("id10", expirationTracker.getExpiredId(time));
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id9"));

        // reestablish the two element state
        expirationTracker.trackExpiration("id10");

        // test forgetting the id after invalidated expiration
        expirationTracker.forget("id9");
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id10"));
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertNull(expirationTracker.getExpiredId(time - 100));
        assertEquals("id10", expirationTracker.getExpiredId(time + 100));


        // reestablish the two element state
        expirationTracker.trackExpiration("id11");
        Thread.sleep(5);
        expirationTracker.trackExpiration("id12");

        // scramble order in the collection with multiple track expiration calls
        expirationTracker.trackExpiration("id11");
        Thread.sleep(3);
        assertEquals(2, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id11"));
        assertTrue(expirationTracker.getTrackedIds().contains("id12"));
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertEquals("id12", expirationTracker.getExpiredId(System.currentTimeMillis()));
    }

    @Test
    public void twoElementApartTrackerEditAlwaysTheLastToExpire() throws InterruptedException {
        // create element tracker with single element
        ExpirationTracker<String> expirationTracker = new ExpirationTracker<>(
                Duration.ofMillis(1));
        expirationTracker.trackExpiration("id1");
        Thread.sleep(5);
        expirationTracker.trackExpiration("id2");

        // expire id
        assertNull(expirationTracker.getExpiredId(System.currentTimeMillis() - 100));
        assertEquals("id1", expirationTracker.getExpiredId(System.currentTimeMillis() + 1));
        assertNull(expirationTracker.getExpiredId(System.currentTimeMillis() - 100));
        assertEquals("id2", expirationTracker.getExpiredId(System.currentTimeMillis() + 1));
        assertNull(expirationTracker.getExpiredId(System.currentTimeMillis()));

        // reestablish previous state of two elements in tracker
        expirationTracker.trackExpiration("id3");
        Thread.sleep(5);
        expirationTracker.trackExpiration("id4");

        // test expiration with get next expiration in millis
        Thread.sleep(1);
        long time = System.currentTimeMillis();
        assertEquals(2, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id3"));
        assertTrue(expirationTracker.getTrackedIds().contains("id4"));
        assertTrue(expirationTracker.getNextExpirationInMillis() <= time);
        assertEquals("id3", expirationTracker.getExpiredId(System.currentTimeMillis()));
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id4"));
        assertEquals("id4", expirationTracker.getExpiredId(System.currentTimeMillis()));
        assertEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertNull(expirationTracker.getExpiredId(System.currentTimeMillis()));
        assertTrue(expirationTracker.getTrackedIds().isEmpty());

        // reestablish previous state of two elements in tracker
        expirationTracker.trackExpiration("id5");
        Thread.sleep(5);
        expirationTracker.trackExpiration("id6");

        // test invalidate expiration
        Thread.sleep(1);
        time = System.currentTimeMillis();
        expirationTracker.invalidateExpiration("non existent");
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        expirationTracker.invalidateExpiration("id6");
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertEquals("id5", expirationTracker.getExpiredId(time));
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id6"));

        // test rescheduling the same id after invalidated expiration
        expirationTracker.trackExpiration("id5");
        Thread.sleep(5);
        // again reestablish the 2 element state in tracker
        expirationTracker.trackExpiration("id6");
        assertEquals(2, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id5"));
        assertTrue(expirationTracker.getTrackedIds().contains("id6"));
        Thread.sleep(1);
        time = System.currentTimeMillis();
        assertTrue(expirationTracker.getNextExpirationInMillis() <= time);
        assertEquals(expirationTracker.getNextExpirationInMillis(), expirationTracker.getNextExpirationInMillis());
        assertEquals("id5", expirationTracker.getExpiredId(time));
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id6"));
        assertEquals("id6", expirationTracker.getExpiredId(time));
        assertEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertNull(expirationTracker.getExpiredId(time));
        assertTrue(expirationTracker.getTrackedIds().isEmpty());

        // reestablish previous state of two elements in tracker
        expirationTracker.trackExpiration("id7");
        Thread.sleep(5);
        expirationTracker.trackExpiration("id8");

        // test forgetting expiration
        expirationTracker.forget("id8");
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id7"));
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertEquals("id7", expirationTracker.getExpiredId(time + 100));

        // reestablish previous state of two elements in tracker
        expirationTracker.trackExpiration("id9");
        Thread.sleep(5);
        expirationTracker.trackExpiration("id10");


        // test invalidate expiration again
        Thread.sleep(1);
        time = System.currentTimeMillis();
        expirationTracker.invalidateExpiration("non existent");
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        expirationTracker.invalidateExpiration("id10");
        assertTrue(expirationTracker.getNextExpirationInMillis() <= time);
        assertEquals("id9", expirationTracker.getExpiredId(time));
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id10"));

        // reestablish the two element state
        expirationTracker.trackExpiration("id9");

        // test forgetting the id after invalidated expiration
        expirationTracker.forget("id10");
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id9"));
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertNull(expirationTracker.getExpiredId(time - 100));
        assertEquals("id9", expirationTracker.getExpiredId(time + 100));

        // reestablish the two element state
        expirationTracker.trackExpiration("id11");
        Thread.sleep(5);
        expirationTracker.trackExpiration("id12");

        // scramble order in the collection with multiple track expiration calls
        // in practice it wont change as we just retrack expiration for id12
        expirationTracker.trackExpiration("id12");
        Thread.sleep(3);
        expirationTracker.trackExpiration("id12");
        expirationTracker.trackExpiration("id12");
        assertEquals(2, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id11"));
        assertTrue(expirationTracker.getTrackedIds().contains("id12"));
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertEquals("id11", expirationTracker.getExpiredId(System.currentTimeMillis()));

        // reestablish the two element state
        expirationTracker.trackExpiration("id13");

        // scramble order in the collection with multiple track expiration calls
        expirationTracker.trackExpiration("id12");
        expirationTracker.trackExpiration("id13");
        expirationTracker.trackExpiration("id13");
        Thread.sleep(3);
        expirationTracker.trackExpiration("id12");
        assertEquals(2, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id12"));
        assertTrue(expirationTracker.getTrackedIds().contains("id13"));
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertEquals("id13", expirationTracker.getExpiredId(System.currentTimeMillis()));
    }

    @Test
    public void twoElementTrackerEditAlwaysTheFirstToExpire() throws InterruptedException {
        // create element tracker with single element
        ExpirationTracker<String> expirationTracker = new ExpirationTracker<>(
                Duration.ofMillis(1));
        expirationTracker.trackExpiration("id1");
        expirationTracker.trackExpiration("id2");

        // expire id
        assertNull(expirationTracker.getExpiredId(System.currentTimeMillis() - 100));
        assertEquals("id1", expirationTracker.getExpiredId(System.currentTimeMillis() + 1));
        assertNull(expirationTracker.getExpiredId(System.currentTimeMillis() - 100));
        assertEquals("id2", expirationTracker.getExpiredId(System.currentTimeMillis() + 1));
        assertNull(expirationTracker.getExpiredId(System.currentTimeMillis()));

        // reestablish previous state of two elements in tracker
        expirationTracker.trackExpiration("id3");
        expirationTracker.trackExpiration("id4");

        // test expiration with get next expiration in millis
        Thread.sleep(1);
        long time = System.currentTimeMillis();
        assertEquals(2, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id3"));
        assertTrue(expirationTracker.getTrackedIds().contains("id4"));
        assertTrue(expirationTracker.getNextExpirationInMillis() <= time);
        assertEquals("id3", expirationTracker.getExpiredId(System.currentTimeMillis()));
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id4"));
        assertEquals("id4", expirationTracker.getExpiredId(System.currentTimeMillis()));
        assertEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertNull(expirationTracker.getExpiredId(System.currentTimeMillis()));
        assertTrue(expirationTracker.getTrackedIds().isEmpty());

        // reestablish previous state of two elements in tracker
        expirationTracker.trackExpiration("id5");
        expirationTracker.trackExpiration("id6");

        // test invalidate expiration
        Thread.sleep(1);
        time = System.currentTimeMillis();
        expirationTracker.invalidateExpiration("non existent");
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        expirationTracker.invalidateExpiration("id5");
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertEquals("id6", expirationTracker.getExpiredId(time));
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id5"));

        // test reescheduling the same id after invalidated expiration
        expirationTracker.trackExpiration("id5");
        // again reestabilish the 2 element state in tracker
        expirationTracker.trackExpiration("id6");
        assertEquals(2, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id5"));
        assertTrue(expirationTracker.getTrackedIds().contains("id6"));
        Thread.sleep(1);
        time = System.currentTimeMillis();
        assertTrue(expirationTracker.getNextExpirationInMillis() <= time);
        assertEquals(expirationTracker.getNextExpirationInMillis(), expirationTracker.getNextExpirationInMillis());
        assertEquals("id5", expirationTracker.getExpiredId(time));
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id6"));
        assertEquals("id6", expirationTracker.getExpiredId(time));
        assertEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertNull(expirationTracker.getExpiredId(time));
        assertTrue(expirationTracker.getTrackedIds().isEmpty());

        // reestablish previous state of two elements in tracker
        expirationTracker.trackExpiration("id7");
        expirationTracker.trackExpiration("id8");

        // test forgetting expiration
        expirationTracker.forget("id7");
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id8"));
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertEquals("id8", expirationTracker.getExpiredId(time + 100));

        // reestablish previous state of two elements in tracker
        expirationTracker.trackExpiration("id9");
        expirationTracker.trackExpiration("id10");


        // test invalidate expiration again
        Thread.sleep(1);
        time = System.currentTimeMillis();
        expirationTracker.invalidateExpiration("non existent");
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        expirationTracker.invalidateExpiration("id9");
        assertTrue(expirationTracker.getNextExpirationInMillis() <= time);
        assertEquals("id10", expirationTracker.getExpiredId(time));
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id9"));

        // reestablish the two element state
        expirationTracker.trackExpiration("id10");

        // test forgetting the id after invalidated expiration
        expirationTracker.forget("id9");
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id10"));
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertNull(expirationTracker.getExpiredId(time - 100));
        assertEquals("id10", expirationTracker.getExpiredId(time + 100));
    }

    @Test
    public void twoElementTrackerEditAlwaysTheLastToExpire() throws InterruptedException {
        // create element tracker with single element
        ExpirationTracker<String> expirationTracker = new ExpirationTracker<>(
                Duration.ofMillis(1));
        expirationTracker.trackExpiration("id1");
        expirationTracker.trackExpiration("id2");

        // expire id
        assertNull(expirationTracker.getExpiredId(System.currentTimeMillis() - 100));
        assertEquals("id1", expirationTracker.getExpiredId(System.currentTimeMillis() + 1));
        assertNull(expirationTracker.getExpiredId(System.currentTimeMillis() - 100));
        assertEquals("id2", expirationTracker.getExpiredId(System.currentTimeMillis() + 1));
        assertNull(expirationTracker.getExpiredId(System.currentTimeMillis()));

        // reestablish previous state of two elements in tracker
        expirationTracker.trackExpiration("id3");
        expirationTracker.trackExpiration("id4");

        // test expiration with get next expiration in millis
        Thread.sleep(1);
        long time = System.currentTimeMillis();
        assertEquals(2, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id3"));
        assertTrue(expirationTracker.getTrackedIds().contains("id4"));
        assertTrue(expirationTracker.getNextExpirationInMillis() <= time);
        assertEquals("id3", expirationTracker.getExpiredId(System.currentTimeMillis()));
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id4"));
        assertEquals("id4", expirationTracker.getExpiredId(System.currentTimeMillis()));
        assertEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertNull(expirationTracker.getExpiredId(System.currentTimeMillis()));
        assertTrue(expirationTracker.getTrackedIds().isEmpty());

        // reestablish previous state of two elements in tracker
        expirationTracker.trackExpiration("id5");
        expirationTracker.trackExpiration("id6");

        // test invalidate expiration
        Thread.sleep(1);
        time = System.currentTimeMillis();
        expirationTracker.invalidateExpiration("non existent");
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        expirationTracker.invalidateExpiration("id6");
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertEquals("id5", expirationTracker.getExpiredId(time));
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id6"));

        // test reescheduling the same id after invalidated expiration
        expirationTracker.trackExpiration("id5");
        // again reestablish the 2 element state in tracker
        expirationTracker.trackExpiration("id6");
        assertEquals(2, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id5"));
        assertTrue(expirationTracker.getTrackedIds().contains("id6"));
        Thread.sleep(1);
        time = System.currentTimeMillis();
        assertTrue(expirationTracker.getNextExpirationInMillis() <= time);
        assertEquals(expirationTracker.getNextExpirationInMillis(), expirationTracker.getNextExpirationInMillis());
        assertEquals("id5", expirationTracker.getExpiredId(time));
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id6"));
        assertEquals("id6", expirationTracker.getExpiredId(time));
        assertEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertNull(expirationTracker.getExpiredId(time));
        assertTrue(expirationTracker.getTrackedIds().isEmpty());

        // reestablish previous state of two elements in tracker
        expirationTracker.trackExpiration("id7");
        expirationTracker.trackExpiration("id8");

        // test forgetting expiration
        expirationTracker.forget("id8");
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id7"));
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertEquals("id7", expirationTracker.getExpiredId(time + 100));

        // reestablish previous state of two elements in tracker
        expirationTracker.trackExpiration("id9");
        expirationTracker.trackExpiration("id10");


        // test invalidate expiration again
        Thread.sleep(1);
        time = System.currentTimeMillis();
        expirationTracker.invalidateExpiration("non existent");
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        expirationTracker.invalidateExpiration("id10");
        assertTrue(expirationTracker.getNextExpirationInMillis() <= time);
        assertEquals("id9", expirationTracker.getExpiredId(time));
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id10"));

        // reestablish the two element state
        expirationTracker.trackExpiration("id9");

        // test forgetting the id after invalidated expiration
        expirationTracker.forget("id10");
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id9"));
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertNull(expirationTracker.getExpiredId(time - 100));
        assertEquals("id9", expirationTracker.getExpiredId(time + 100));
    }

    @Test
    public void threeElementTrackerEditAlwaysTheMiddle() throws InterruptedException {
        // create element tracker with single element
        ExpirationTracker<String> expirationTracker = new ExpirationTracker<>(
                Duration.ofMillis(1));
        expirationTracker.trackExpiration("id1");
        Thread.sleep(5);
        expirationTracker.trackExpiration("id2");
        Thread.sleep(5);
        expirationTracker.trackExpiration("id3");

        // expire ids
        assertNull(expirationTracker.getExpiredId(System.currentTimeMillis() - 100));
        assertEquals("id1", expirationTracker.getExpiredId(System.currentTimeMillis() + 1));
        assertNull(expirationTracker.getExpiredId(System.currentTimeMillis() - 100));
        assertEquals("id2", expirationTracker.getExpiredId(System.currentTimeMillis() + 1));
        assertNull(expirationTracker.getExpiredId(System.currentTimeMillis() - 100));
        assertEquals("id3", expirationTracker.getExpiredId(System.currentTimeMillis() + 1));
        assertNull(expirationTracker.getExpiredId(System.currentTimeMillis() + 1));

        // reestablish previous state of three elements in tracker
        expirationTracker.trackExpiration("id4");
        Thread.sleep(5);
        expirationTracker.trackExpiration("id5");
        Thread.sleep(5);
        expirationTracker.trackExpiration("id6");

        // test expiration with get next expiration in millis
        Thread.sleep(10);
        long time = System.currentTimeMillis();
        assertEquals(3, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id4"));
        assertTrue(expirationTracker.getTrackedIds().contains("id5"));
        assertTrue(expirationTracker.getTrackedIds().contains("id6"));
        assertTrue(expirationTracker.getNextExpirationInMillis() <= time);
        assertEquals("id4", expirationTracker.getExpiredId(System.currentTimeMillis()));
        assertEquals(2, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id5"));
        assertTrue(expirationTracker.getTrackedIds().contains("id6"));
        assertEquals("id5", expirationTracker.getExpiredId(System.currentTimeMillis()));
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id6"));
        assertEquals("id6", expirationTracker.getExpiredId(System.currentTimeMillis()));
        assertEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertNull(expirationTracker.getExpiredId(System.currentTimeMillis()));
        assertTrue(expirationTracker.getTrackedIds().isEmpty());

        // reestablish previous state of three elements in tracker
        expirationTracker.trackExpiration("id7");
        Thread.sleep(5);
        expirationTracker.trackExpiration("id8");
        Thread.sleep(5);
        expirationTracker.trackExpiration("id9");

        // test invalidate expiration
        Thread.sleep(10);
        time = System.currentTimeMillis();
        expirationTracker.invalidateExpiration("non existent");
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        expirationTracker.invalidateExpiration("id8");
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertEquals("id7", expirationTracker.getExpiredId(time));
        assertEquals(2, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id8"));
        assertTrue(expirationTracker.getTrackedIds().contains("id9"));
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertEquals("id9", expirationTracker.getExpiredId(time));
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id8"));
        assertEquals(-1, expirationTracker.getNextExpirationInMillis());

        // test rescheduling the same id after invalidated expiration
        // and, again reestablish the 3 element state in tracker
        expirationTracker.trackExpiration("id7");
        Thread.sleep(5);
        expirationTracker.trackExpiration("id8");
        Thread.sleep(5);
        expirationTracker.trackExpiration("id9");

        assertEquals(3, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id7"));
        assertTrue(expirationTracker.getTrackedIds().contains("id8"));
        assertTrue(expirationTracker.getTrackedIds().contains("id9"));
        Thread.sleep(10);
        time = System.currentTimeMillis();
        assertTrue(expirationTracker.getNextExpirationInMillis() <= time);
        assertEquals(expirationTracker.getNextExpirationInMillis(), expirationTracker.getNextExpirationInMillis());
        assertEquals("id7", expirationTracker.getExpiredId(time));
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertEquals(2, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id8"));
        assertTrue(expirationTracker.getTrackedIds().contains("id9"));
        assertTrue(expirationTracker.getNextExpirationInMillis() <= time);
        assertEquals("id8", expirationTracker.getExpiredId(time));
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id9"));
        assertTrue(expirationTracker.getNextExpirationInMillis() <= time);
        assertEquals("id9", expirationTracker.getExpiredId(time));
        assertEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertNull(expirationTracker.getExpiredId(time));
        assertTrue(expirationTracker.getTrackedIds().isEmpty());

        // reestablish previous state of three elements in tracker
        expirationTracker.trackExpiration("id10");
        Thread.sleep(5);
        expirationTracker.trackExpiration("id11");
        Thread.sleep(5);
        expirationTracker.trackExpiration("id12");

        // test forgetting expiration
        expirationTracker.forget("id11");
        assertEquals(2, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id10"));
        assertTrue(expirationTracker.getTrackedIds().contains("id12"));
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertEquals("id10", expirationTracker.getExpiredId(time + 100));
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertEquals("id12", expirationTracker.getExpiredId(time + 100));

        // reestablish previous state of three elements in tracker
        expirationTracker.trackExpiration("id13");
        Thread.sleep(5);
        expirationTracker.trackExpiration("id14");
        Thread.sleep(5);
        expirationTracker.trackExpiration("id15");


        // test invalidate expiration again
        Thread.sleep(10);
        time = System.currentTimeMillis();
        expirationTracker.invalidateExpiration("non existent");
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());
        expirationTracker.invalidateExpiration("id14");
        assertTrue(expirationTracker.getNextExpirationInMillis() <= time);
        assertEquals("id13", expirationTracker.getExpiredId(time));
        assertEquals(2, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id14"));
        assertTrue(expirationTracker.getTrackedIds().contains("id15"));
        assertTrue(expirationTracker.getNextExpirationInMillis() <= time);
        assertEquals("id15", expirationTracker.getExpiredId(time));
        assertEquals(1, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id14"));
        assertEquals(-1, expirationTracker.getNextExpirationInMillis());
        assertNull(expirationTracker.getExpiredId(time));

        // reestablish the three element state
        expirationTracker.trackExpiration("id13");
        Thread.sleep(5);
        expirationTracker.trackExpiration("id15");

        // test forgetting the id after invalidated expiration
        expirationTracker.forget("id14");
        assertEquals(2, expirationTracker.getTrackedIds().size());
        assertTrue(expirationTracker.getTrackedIds().contains("id13"));
        assertTrue(expirationTracker.getTrackedIds().contains("id15"));
        assertNotEquals(-1, expirationTracker.getNextExpirationInMillis());

        // reestablish the three element state
        expirationTracker.trackExpiration("id16");
        expirationTracker.invalidateExpiration("id13");
        expirationTracker.invalidateExpiration("id15");
        assertNull(expirationTracker.getExpiredId(time - 100));
        assertEquals("id16", expirationTracker.getExpiredId(time + 100));

        // scramble the order and add and remove nodes
        expirationTracker.trackExpiration("id17");
        Thread.sleep(5);
        expirationTracker.trackExpiration("id18");
        Thread.sleep(5);
        expirationTracker.trackExpiration("id19");
        Thread.sleep(10);
        expirationTracker.trackExpiration("id18");
        expirationTracker.trackExpiration("id18");
        expirationTracker.trackExpiration("id19");
        Thread.sleep(1);
        expirationTracker.trackExpiration("id17");
        expirationTracker.trackExpiration("id19");
        expirationTracker.forget("id13");
        expirationTracker.trackExpiration("id15");
        time = System.currentTimeMillis();
        assertNull(expirationTracker.getExpiredId(time - 100));
        assertEquals("id18", expirationTracker.getExpiredId(time + 100));
        assertEquals("id17", expirationTracker.getExpiredId(time + 100));
        assertEquals("id19", expirationTracker.getExpiredId(time + 100));
        expirationTracker.trackExpiration("id13");
        assertEquals("id15", expirationTracker.getExpiredId(time + 100));
        assertEquals("id13", expirationTracker.getExpiredId(time + 100));
        assertNull(expirationTracker.getExpiredId(time + 100));
        assertTrue(expirationTracker.getTrackedIds().isEmpty());

    }
}
