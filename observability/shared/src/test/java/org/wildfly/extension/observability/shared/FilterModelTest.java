/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.observability.shared;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.jboss.dmr.ModelNode;
import org.junit.Test;
import org.wildfly.extension.observability.shared.FilterModel.Condition;
import org.wildfly.extension.observability.shared.FilterModel.Field;
import org.wildfly.extension.observability.shared.FilterModel.Outcome;

public class FilterModelTest {

    private static final String JVM_MEMORY_NAME = "jvm.memory.used";
    private static final Map<String, String> JVM_MEMORY_TAGS = Map.of("env", "prod");

    private static final String JVM_GC_NAME = "jvm.gc.pause";
    private static final Map<String, String> JVM_GC_TAGS = Map.of("env", "staging");

    private static final String HTTP_SERVER_NAME = "http.server.requests";
    private static final Map<String, String> HTTP_SERVER_TAGS = Map.of("method", "GET", "status", "200");

    private static final String HTTP_CLIENT_NAME = "http.client.requests";
    private static final Map<String, String> HTTP_CLIENT_TAGS = Map.of("method", "POST", "status", "500");

    private static final String CUSTOM_NAME = "custom.metric.count";
    private static final Map<String, String> CUSTOM_TAGS = Map.of("priority", "high");

    private static final String DATABASE_NAME = "database.query.time";
    private static final Map<String, String> DATABASE_TAGS = Map.of("pool", "primary");

    // ==================== FilterModel.of() factory ====================

    @Test
    public void testOfFactory() {
        ModelNode model = new ModelNode();
        model.get("outcome").set("accept");
        model.get("field").set("meter-name");
        model.get("condition").set("starts-with");
        model.get("negate").set(true);
        model.get("value").set("jvm.");

        FilterModel filter = FilterModel.of(model);
        assertEquals(Outcome.ACCEPT, filter.outcome());
        assertEquals(Field.METER_NAME, filter.field());
        assertEquals(Condition.STARTS_WITH, filter.condition());
        assertTrue(filter.negate());
        assertEquals("jvm.", filter.value());
    }

    @Test
    public void testOfFactoryDefaults() {
        ModelNode model = new ModelNode();
        model.get("field").set("tag-name");
        model.get("condition").set("equals");
        model.get("value").set("env");

        FilterModel filter = FilterModel.of(model);
        assertEquals(Outcome.REJECT, filter.outcome());
        assertFalse(filter.negate());
    }

    @Test
    public void testOfFactoryOutcomeCaseInsensitive() {
        ModelNode model = new ModelNode();
        model.get("outcome").set("accept");
        model.get("field").set("tag-value");
        model.get("condition").set("contains");
        model.get("value").set("prod");

        FilterModel filter = FilterModel.of(model);
        assertEquals(Outcome.ACCEPT, filter.outcome());
    }

    // ==================== Enum parsing ====================

    @Test
    public void testConditionValues() {
        assertEquals(4, Condition.values().length);
        assertNotNull(Condition.of("starts-with"));
        assertNotNull(Condition.of("ends-with"));
        assertNotNull(Condition.of("contains"));
        assertNotNull(Condition.of("equals"));
    }

    @Test
    public void testOutcomeValues() {
        assertEquals(2, Outcome.values().length);
        assertNotNull(Outcome.of("accept"));
        assertNotNull(Outcome.of("reject"));
    }

    @Test
    public void testFieldValues() {
        assertEquals(3, Field.values().length);
        assertNotNull(Field.of("meter-name"));
        assertNotNull(Field.of("tag-name"));
        assertNotNull(Field.of("tag-value"));
    }

    // ==================== Field.of() ====================

    @Test
    public void testFieldOfMeterName() {
        assertEquals(Field.METER_NAME, Field.of("meter-name"));
    }

    @Test
    public void testFieldOfTagName() {
        assertEquals(Field.TAG_NAME, Field.of("tag-name"));
    }

    @Test
    public void testFieldOfTagValue() {
        assertEquals(Field.TAG_VALUE, Field.of("tag-value"));
    }

    // ==================== Enum getValue() ====================

    @Test
    public void testGetValue() {
        assertEquals("accept", Outcome.ACCEPT.getValue());
        assertEquals("reject", Outcome.REJECT.getValue());
        assertEquals("meter-name", Field.METER_NAME.getValue());
        assertEquals("tag-name", Field.TAG_NAME.getValue());
        assertEquals("tag-value", Field.TAG_VALUE.getValue());
        assertEquals("starts-with", Condition.STARTS_WITH.getValue());
        assertEquals("ends-with", Condition.ENDS_WITH.getValue());
        assertEquals("contains", Condition.CONTAINS.getValue());
        assertEquals("equals", Condition.EQUALS.getValue());
    }

    // ==================== matchesCondition() ====================

    @Test
    public void testMatchesConditionStartsWith() {
        assertTrue(FilterModel.matchesCondition(Condition.STARTS_WITH, "jvm.memory.used", "jvm."));
        assertFalse(FilterModel.matchesCondition(Condition.STARTS_WITH, "http.server", "jvm."));
    }

    @Test
    public void testMatchesConditionEndsWith() {
        assertTrue(FilterModel.matchesCondition(Condition.ENDS_WITH, "http.server.requests", ".requests"));
        assertFalse(FilterModel.matchesCondition(Condition.ENDS_WITH, "jvm.memory.used", ".requests"));
    }

    @Test
    public void testMatchesConditionContains() {
        assertTrue(FilterModel.matchesCondition(Condition.CONTAINS, "jvm.memory.used", "memory"));
        assertFalse(FilterModel.matchesCondition(Condition.CONTAINS, "http.server.requests", "memory"));
    }

    @Test
    public void testMatchesConditionEquals() {
        assertTrue(FilterModel.matchesCondition(Condition.EQUALS, "custom.metric.count", "custom.metric.count"));
        assertFalse(FilterModel.matchesCondition(Condition.EQUALS, "custom.metric.count", "custom.metric"));
    }

    @Test
    public void testMatchesConditionCaseSensitive() {
        assertFalse(FilterModel.matchesCondition(Condition.EQUALS, "JVM", "jvm"));
        assertFalse(FilterModel.matchesCondition(Condition.STARTS_WITH, "JVM.memory", "jvm"));
        assertFalse(FilterModel.matchesCondition(Condition.CONTAINS, "JVM.memory", "jvm"));
    }

    // ==================== matches() — METER_NAME ====================

    @Test
    public void testMatchesMeterNameStartsWith() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.METER_NAME, Condition.STARTS_WITH, false, "jvm.");
        assertTrue(filter.matches(JVM_MEMORY_NAME, JVM_MEMORY_TAGS));
        assertTrue(filter.matches(JVM_GC_NAME, JVM_GC_TAGS));
        assertFalse(filter.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));
        assertFalse(filter.matches(CUSTOM_NAME, CUSTOM_TAGS));
    }

    @Test
    public void testMatchesMeterNameStartsWithNegate() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.METER_NAME, Condition.STARTS_WITH, true, "jvm.");
        assertFalse(filter.matches(JVM_MEMORY_NAME, JVM_MEMORY_TAGS));
        assertFalse(filter.matches(JVM_GC_NAME, JVM_GC_TAGS));
        assertTrue(filter.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));
        assertTrue(filter.matches(CUSTOM_NAME, CUSTOM_TAGS));
    }

    @Test
    public void testMatchesMeterNameEndsWith() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.METER_NAME, Condition.ENDS_WITH, false, ".requests");
        assertTrue(filter.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));
        assertTrue(filter.matches(HTTP_CLIENT_NAME, HTTP_CLIENT_TAGS));
        assertFalse(filter.matches(JVM_MEMORY_NAME, JVM_MEMORY_TAGS));
        assertFalse(filter.matches(DATABASE_NAME, DATABASE_TAGS));
    }

    @Test
    public void testMatchesMeterNameEndsWithNegate() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.METER_NAME, Condition.ENDS_WITH, true, ".requests");
        assertFalse(filter.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));
        assertFalse(filter.matches(HTTP_CLIENT_NAME, HTTP_CLIENT_TAGS));
        assertTrue(filter.matches(JVM_MEMORY_NAME, JVM_MEMORY_TAGS));
        assertTrue(filter.matches(DATABASE_NAME, DATABASE_TAGS));
    }

    @Test
    public void testMatchesMeterNameContains() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.METER_NAME, Condition.CONTAINS, false, "http");
        assertTrue(filter.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));
        assertTrue(filter.matches(HTTP_CLIENT_NAME, HTTP_CLIENT_TAGS));
        assertFalse(filter.matches(JVM_MEMORY_NAME, JVM_MEMORY_TAGS));
        assertFalse(filter.matches(DATABASE_NAME, DATABASE_TAGS));
    }

    @Test
    public void testMatchesMeterNameContainsNegate() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.METER_NAME, Condition.CONTAINS, true, "http");
        assertFalse(filter.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));
        assertFalse(filter.matches(HTTP_CLIENT_NAME, HTTP_CLIENT_TAGS));
        assertTrue(filter.matches(JVM_MEMORY_NAME, JVM_MEMORY_TAGS));
        assertTrue(filter.matches(DATABASE_NAME, DATABASE_TAGS));
    }

    @Test
    public void testMatchesMeterNameEquals() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.METER_NAME, Condition.EQUALS, false, "custom.metric.count");
        assertTrue(filter.matches(CUSTOM_NAME, CUSTOM_TAGS));
        assertFalse(filter.matches(JVM_MEMORY_NAME, JVM_MEMORY_TAGS));
        assertFalse(filter.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));
    }

    @Test
    public void testMatchesMeterNameEqualsNegate() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.METER_NAME, Condition.EQUALS, true, "custom.metric.count");
        assertFalse(filter.matches(CUSTOM_NAME, CUSTOM_TAGS));
        assertTrue(filter.matches(JVM_MEMORY_NAME, JVM_MEMORY_TAGS));
        assertTrue(filter.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));
    }

    // ==================== matches() — TAG_NAME ====================

    @Test
    public void testMatchesTagNameStartsWith() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.TAG_NAME, Condition.STARTS_WITH, false, "meth");
        assertTrue(filter.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));
        assertTrue(filter.matches(HTTP_CLIENT_NAME, HTTP_CLIENT_TAGS));
        assertFalse(filter.matches(JVM_MEMORY_NAME, JVM_MEMORY_TAGS));
        assertFalse(filter.matches(DATABASE_NAME, DATABASE_TAGS));
    }

    @Test
    public void testMatchesTagNameStartsWithNegate() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.TAG_NAME, Condition.STARTS_WITH, true, "env");
        assertTrue(filter.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));
        assertFalse(filter.matches(JVM_MEMORY_NAME, JVM_MEMORY_TAGS));
        assertFalse(filter.matches(JVM_GC_NAME, JVM_GC_TAGS));
    }

    @Test
    public void testMatchesTagNameEndsWith() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.TAG_NAME, Condition.ENDS_WITH, false, "ool");
        assertTrue(filter.matches(DATABASE_NAME, DATABASE_TAGS));
        assertFalse(filter.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));
        assertFalse(filter.matches(JVM_MEMORY_NAME, JVM_MEMORY_TAGS));
    }

    @Test
    public void testMatchesTagNameEndsWithNegate() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.TAG_NAME, Condition.ENDS_WITH, true, "ool");
        assertFalse(filter.matches(DATABASE_NAME, DATABASE_TAGS));
        assertTrue(filter.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));
        assertTrue(filter.matches(JVM_MEMORY_NAME, JVM_MEMORY_TAGS));
    }

    @Test
    public void testMatchesTagNameContains() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.TAG_NAME, Condition.CONTAINS, false, "stat");
        assertTrue(filter.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));
        assertTrue(filter.matches(HTTP_CLIENT_NAME, HTTP_CLIENT_TAGS));
        assertFalse(filter.matches(JVM_MEMORY_NAME, JVM_MEMORY_TAGS));
        assertFalse(filter.matches(DATABASE_NAME, DATABASE_TAGS));
    }

    @Test
    public void testMatchesTagNameContainsNegate() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.TAG_NAME, Condition.CONTAINS, true, "env");
        assertTrue(filter.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));
        assertFalse(filter.matches(JVM_MEMORY_NAME, JVM_MEMORY_TAGS));
        assertFalse(filter.matches(JVM_GC_NAME, JVM_GC_TAGS));
    }

    @Test
    public void testMatchesTagNameEquals() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.TAG_NAME, Condition.EQUALS, false, "priority");
        assertTrue(filter.matches(CUSTOM_NAME, CUSTOM_TAGS));
        assertFalse(filter.matches(JVM_MEMORY_NAME, JVM_MEMORY_TAGS));
        assertFalse(filter.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));
    }

    @Test
    public void testMatchesTagNameEqualsNegate() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.TAG_NAME, Condition.EQUALS, true, "priority");
        assertFalse(filter.matches(CUSTOM_NAME, CUSTOM_TAGS));
        assertTrue(filter.matches(JVM_MEMORY_NAME, JVM_MEMORY_TAGS));
        assertTrue(filter.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));
    }

    // ==================== matches() — TAG_VALUE ====================

    @Test
    public void testMatchesTagValueStartsWith() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.TAG_VALUE, Condition.STARTS_WITH, false, "pro");
        assertTrue(filter.matches(JVM_MEMORY_NAME, JVM_MEMORY_TAGS));
        assertFalse(filter.matches(JVM_GC_NAME, JVM_GC_TAGS));
        assertFalse(filter.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));
    }

    @Test
    public void testMatchesTagValueStartsWithNegate() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.TAG_VALUE, Condition.STARTS_WITH, true, "pro");
        assertFalse(filter.matches(JVM_MEMORY_NAME, JVM_MEMORY_TAGS));
        assertTrue(filter.matches(JVM_GC_NAME, JVM_GC_TAGS));
        assertTrue(filter.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));
    }

    @Test
    public void testMatchesTagValueEndsWith() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.TAG_VALUE, Condition.ENDS_WITH, false, "ing");
        assertTrue(filter.matches(JVM_GC_NAME, JVM_GC_TAGS));
        assertFalse(filter.matches(JVM_MEMORY_NAME, JVM_MEMORY_TAGS));
        assertFalse(filter.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));
    }

    @Test
    public void testMatchesTagValueEndsWithNegate() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.TAG_VALUE, Condition.ENDS_WITH, true, "ing");
        assertFalse(filter.matches(JVM_GC_NAME, JVM_GC_TAGS));
        assertTrue(filter.matches(JVM_MEMORY_NAME, JVM_MEMORY_TAGS));
        assertTrue(filter.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));
    }

    @Test
    public void testMatchesTagValueContains() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.TAG_VALUE, Condition.CONTAINS, false, "GET");
        assertTrue(filter.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));
        assertFalse(filter.matches(HTTP_CLIENT_NAME, HTTP_CLIENT_TAGS));
        assertFalse(filter.matches(JVM_MEMORY_NAME, JVM_MEMORY_TAGS));
    }

    @Test
    public void testMatchesTagValueContainsNegate() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.TAG_VALUE, Condition.CONTAINS, true, "GET");
        assertFalse(filter.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));
        assertTrue(filter.matches(HTTP_CLIENT_NAME, HTTP_CLIENT_TAGS));
        assertTrue(filter.matches(JVM_MEMORY_NAME, JVM_MEMORY_TAGS));
    }

    @Test
    public void testMatchesTagValueEquals() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.TAG_VALUE, Condition.EQUALS, false, "primary");
        assertTrue(filter.matches(DATABASE_NAME, DATABASE_TAGS));
        assertFalse(filter.matches(JVM_MEMORY_NAME, JVM_MEMORY_TAGS));
        assertFalse(filter.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));
    }

    @Test
    public void testMatchesTagValueEqualsNegate() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.TAG_VALUE, Condition.EQUALS, true, "primary");
        assertFalse(filter.matches(DATABASE_NAME, DATABASE_TAGS));
        assertTrue(filter.matches(JVM_MEMORY_NAME, JVM_MEMORY_TAGS));
        assertTrue(filter.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));
    }

    // ==================== matches() — multi-tag and boundary ====================

    @Test
    public void testMatchesTagValueAnyTag() {
        FilterModel filterGet = new FilterModel(Outcome.REJECT, Field.TAG_VALUE, Condition.EQUALS, false, "GET");
        assertTrue(filterGet.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));

        FilterModel filter200 = new FilterModel(Outcome.REJECT, Field.TAG_VALUE, Condition.EQUALS, false, "200");
        assertTrue(filter200.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));
    }

    @Test
    public void testMatchesTagNameAnyTag() {
        FilterModel filterMethod = new FilterModel(Outcome.REJECT, Field.TAG_NAME, Condition.EQUALS, false, "method");
        assertTrue(filterMethod.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));

        FilterModel filterStatus = new FilterModel(Outcome.REJECT, Field.TAG_NAME, Condition.EQUALS, false, "status");
        assertTrue(filterStatus.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));
    }

    @Test
    public void testMatchesNoMatch() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.METER_NAME, Condition.STARTS_WITH, false, "nonexistent.");
        assertFalse(filter.matches(JVM_MEMORY_NAME, JVM_MEMORY_TAGS));
        assertFalse(filter.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));
    }

    @Test
    public void testMatchesAllMatch() {
        FilterModel filter = new FilterModel(Outcome.REJECT, Field.METER_NAME, Condition.CONTAINS, false, ".");
        assertTrue(filter.matches(JVM_MEMORY_NAME, JVM_MEMORY_TAGS));
        assertTrue(filter.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));
        assertTrue(filter.matches(CUSTOM_NAME, CUSTOM_TAGS));
        assertTrue(filter.matches(DATABASE_NAME, DATABASE_TAGS));
    }

    @Test
    public void testMatchesOutcomeDoesNotAffectResult() {
        FilterModel accept = new FilterModel(Outcome.ACCEPT, Field.METER_NAME, Condition.STARTS_WITH, false, "jvm.");
        FilterModel reject = new FilterModel(Outcome.REJECT, Field.METER_NAME, Condition.STARTS_WITH, false, "jvm.");
        assertEquals(accept.matches(JVM_MEMORY_NAME, JVM_MEMORY_TAGS), reject.matches(JVM_MEMORY_NAME, JVM_MEMORY_TAGS));
        assertEquals(accept.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS), reject.matches(HTTP_SERVER_NAME, HTTP_SERVER_TAGS));
    }
}
