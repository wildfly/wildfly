/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.timerservice.schedule;

import org.jboss.as.ejb3.timerservice.schedule.value.IncrementValue;
import org.jboss.as.ejb3.timerservice.schedule.value.RangeValue;
import org.junit.Assert;
import org.junit.Test;

public class ScheduleValueTestCase {

    @Test
    public void testInvalidRange() {
        String[] invalidRangeValues =
                {null, "", " ", "0.1", "1d", "1.0", "?", "%", "$", "!", "&", "-", "/", ",", ".", "1-", "1-2-3", "1+2", "**",
                        "*-", "*,1", "1,*", "5/*", "1, 2/2", "---", "-", "--", " -2 -3 -4", "-0", "1--"};
        for (String invalidRange : invalidRangeValues) {
            boolean accepts = RangeValue.accepts(invalidRange);
            Assert.assertFalse("Range value accepted an invalid value: " + invalidRange, accepts);

            try {
                RangeValue invalidRangeValue = new RangeValue(invalidRange);
                Assert.fail("Range value did *not* throw IllegalArgumentException for an invalid range: " + invalidRange);
            } catch (IllegalArgumentException iae) {
                // expected
            }
        }
    }

    @Test
    public void testValidRange() {
        String[] validRanges =
                {"1-8", "-7--1", "7--1", "1st Fri-1st Mon"};
        for (String validRange : validRanges) {
            boolean accepts = RangeValue.accepts(validRange);
            Assert.assertTrue("Valid range value wasn't accepted: " + validRange, accepts);
            RangeValue validRangeValue = new RangeValue(validRange);
        }
    }

    @Test
    public void testInvalidIncrement() {
        String[] invalidValues = {
                "1/-1", "1/10.0", "10/*", "10/?", "10/", "10/-",
                "/10", "?/10", "-/10", "**/10", "10.0/10"
        };
        for (String v : invalidValues) {
            try {
                new IncrementValue(v);
                Assert.fail("Failed to get IllegalArgumentException for invalid increment value: " + v);
            } catch (IllegalArgumentException e) {
                // expected
            }
        }
    }
}
