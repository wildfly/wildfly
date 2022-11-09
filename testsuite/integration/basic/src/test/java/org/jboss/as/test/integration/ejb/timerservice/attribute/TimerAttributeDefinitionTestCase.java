package org.jboss.as.test.integration.ejb.timerservice.attribute;

import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.ejb3.subsystem.deployment.TimerAttributeDefinition;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Test;

import java.util.Locale;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNIT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TimerAttributeDefinitionTestCase {

    @Test
    public void testTimerAttributeDefinition(){
        SimpleOperationDefinitionBuilder builder = new SimpleOperationDefinitionBuilder("operation",
                new StandardResourceDescriptionResolver("simple", TimerAttributeDefinitionTestCase.class.getName(), Thread.currentThread().getContextClassLoader()));
        builder.addParameter(create("parameter", ModelType.STRING, true).setAllowExpression(true).build());
        builder.setReplyType(ModelType.LIST);
        builder.setReplyParameters(new TimerAttributeDefinition.Builder().build());
        ModelNode description = builder.build().getDescriptionProvider().getModelDescription(Locale.ROOT);

        assertTrue(description.get(REPLY_PROPERTIES, VALUE_TYPE, "timers").isDefined());
        assertTrue(description.get(REPLY_PROPERTIES, VALUE_TYPE, "timers", VALUE_TYPE).isDefined());

        ModelNode timersValueType = description.get(REPLY_PROPERTIES, VALUE_TYPE, "timers", VALUE_TYPE);

        valueTypeCheck(timersValueType, "time-remaining");
        valueTypeCheck(timersValueType, "next-timeout");
        valueTypeCheck(timersValueType, "calendar-timer");
        valueTypeCheck(timersValueType, "info");

        ModelNode scheduleValueType = timersValueType.get("schedule", VALUE_TYPE);

        valueTypeCheck(scheduleValueType, "year");
        valueTypeCheck(scheduleValueType, "month");
        valueTypeCheck(scheduleValueType, "day-of-month");
        valueTypeCheck(scheduleValueType, "day-of-week");
        valueTypeCheck(scheduleValueType, "hour");
        valueTypeCheck(scheduleValueType, "minute");
        valueTypeCheck(scheduleValueType, "second");
        valueTypeCheck(scheduleValueType, "timezone");
        valueTypeCheck(scheduleValueType, "start");
        valueTypeCheck(scheduleValueType, "end");

        assertTrue(timersValueType.get("persistent", TYPE).isDefined());
        assertFalse(timersValueType.get("value-type persistent should not have defined description", "persistent", DESCRIPTION).isDefined());
    }

    private void valueTypeCheck(ModelNode modelNode, String valueType){
        assertTrue("Type is undefined for value-type " + valueType, modelNode.get(valueType, TYPE).isDefined());
        assertTrue("Description is undefined for value-type " + valueType, modelNode.get(valueType, DESCRIPTION).isDefined());
        assertFalse("value-type " + valueType + " should not have Nillable defined", modelNode.get(valueType, NILLABLE).isDefined());
        assertFalse("value-type " + valueType + " should not have Unit defined", modelNode.get(valueType, UNIT).isDefined());
    }

}
