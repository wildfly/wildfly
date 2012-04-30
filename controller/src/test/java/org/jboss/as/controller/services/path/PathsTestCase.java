/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors.
*
* This is free software; you can redistribute it and/or modify it
* under the terms of the GNU Lesser General Public License as
* published by the Free Software Foundation; either version 2.1 of
* the License, or (at your option) any later version.
*
* This software is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public
* License along with this software; if not, write to the Free
* Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
* 02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jboss.as.controller.services.path;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_RESOURCES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_CHILDREN_TYPES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ONLY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_OPERATION_NAMES_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_DESCRIPTION_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RECURSIVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.CommonProviders;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry.Flag;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManager.Event;
import org.jboss.as.controller.services.path.PathManager.PathEventContext;
import org.jboss.as.controller.test.AbstractControllerTestBase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.ServiceController.Transition;
import org.jboss.msc.service.ServiceName;
import org.junit.Test;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class PathsTestCase extends AbstractControllerTestBase {

    PathManagerService pathManagerService;

    @Test
    public void testReadResourceDescription() throws Exception{
        //Just a sanity check to make sure the resource description can be read
        ModelNode operation = createOperation(READ_RESOURCE_DESCRIPTION_OPERATION);
        operation.get(RECURSIVE).set(true);
        operation.get(OPERATIONS).set(true);

        ModelNode result = executeForResult(operation);
    }

    @Test
    public void testAddPath() throws Exception {

        ModelNode result = readResource();
        Assert.assertEquals(1, result.get(PATH).keys().size());
        checkPath(result, "hardcoded", "/hard/coded", null, true);

        ModelNode operation = createOperation(ADD);
        operation.get(OP_ADDR).add(PATH, "add1");
        operation.get(PATH).set("xyz");
        executeForResult(operation);

        result = readResource();
        Assert.assertTrue(result.hasDefined(PATH));
        Assert.assertEquals(2, result.get(PATH).keys().size());
        checkPath(result, "hardcoded", "/hard/coded", null, true);
        checkPath(result, "add1", "xyz", null, false);

        operation = createOperation(ADD);
        operation.get(OP_ADDR).add(PATH, "add2");
        operation.get(PATH).set("123");
        operation.get(RELATIVE_TO).set("add1");
        executeForResult(operation);

        result = readResource();
        Assert.assertTrue(result.hasDefined(PATH));
        Assert.assertEquals(3, result.get(PATH).keys().size());
        checkPath(result, "hardcoded", "/hard/coded", null, true);
        checkPath(result, "add1", "xyz", null, false);
        checkPath(result, "add2", "123", "add1", false);
    }

    @Test
    public void testRemovePath() throws Exception {
        testAddPath();
        ModelNode operation = createOperation(REMOVE);
        operation.get(OP_ADDR).add(PATH, "add2");
        executeForResult(operation);

        ModelNode result = readResource();
        Assert.assertTrue(result.hasDefined(PATH));
        Assert.assertEquals(2, result.get(PATH).keys().size());
        checkPath(result, "hardcoded", "/hard/coded", null, true);
        checkPath(result, "add1", "xyz", null, false);

        operation = createOperation(REMOVE);
        operation.get(OP_ADDR).add(PATH, "add1");
        executeForResult(operation);

        result = readResource();
        checkPath(result, "hardcoded", "/hard/coded", null, true);
    }

    @Test
    public void testChangeAbsolutePath() throws Exception {
        testAddPath();
        ModelNode operation = createOperation(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).add(PATH, "add1");
        operation.get(NAME).set(PATH);
        operation.get(VALUE).set("updated");
        executeForResult(operation);

        checkPath(readResource(), "add1", "updated", null, false);
    }

    @Test
    public void testChangeRelativePath() throws Exception {
        testAddPath();
        ModelNode operation = createOperation(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).add(PATH, "add2");
        operation.get(NAME).set(PATH);
        operation.get(VALUE).set("updated");
        executeForResult(operation);

        checkPath(readResource(), "add2", "updated", "add1", false);
    }

    @Test
    public void testChangeBetweenRelativeAndAbsolutePaths() throws Exception {
        testAddPath();
        ModelNode operation = createOperation(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).add(PATH, "add2");
        operation.get(NAME).set(RELATIVE_TO);
        operation.get(VALUE).set(new ModelNode());
        executeForResult(operation);

        checkPath(readResource(), "add2", "123", null, false);

        operation = createOperation(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).add(PATH, "add2");
        operation.get(NAME).set(RELATIVE_TO);
        operation.get(VALUE).set("add1");
        executeForResult(operation);

        checkPath(readResource(), "add2", "123", "add1", false);

        operation = createOperation(UNDEFINE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).add(PATH, "add2");
        operation.get(NAME).set(RELATIVE_TO);
        executeForResult(operation);

        checkPath(readResource(), "add2", "123", null, false);
    }

    @Test
    public void testRemoveHardcodedPathFails() throws Exception {
        ModelNode result = readResource();
        Assert.assertEquals(1, result.get(PATH).keys().size());
        checkPath(result, "hardcoded", "/hard/coded", null, true);

        ModelNode operation = createOperation(REMOVE);
        operation.get(OP_ADDR).add(PATH, "hardcoded");
        try {
            executeForResult(operation);
            Assert.fail("Removing a read-only path should have failed");
        } catch (OperationFailedException expected) {
        }

        checkPath(result, "hardcoded", "/hard/coded", null, true);
    }

    @Test
    public void testChangeHardcodedPathFails() throws Exception {
        testAddPath();
        checkPath(readResource(), "hardcoded", "/hard/coded", null, true);

        ModelNode operation = createOperation(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).add(PATH, "hardcoded");
        operation.get(NAME).set(RELATIVE_TO);
        operation.get(VALUE).set("add1");
        try {
            executeForResult(operation);
            Assert.fail("Changing a read-only path should have failed");
        } catch (OperationFailedException expected) {
        }
        checkPath(readResource(), "hardcoded", "/hard/coded", null, true);

        operation = createOperation(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).add(PATH, "hardcoded");
        operation.get(NAME).set(PATH);
        operation.get(VALUE).set("abc");
        try {
            executeForResult(operation);
            Assert.fail("Changing a read-only path should have failed");
        } catch (OperationFailedException expected) {
        }
        checkPath(readResource(), "hardcoded", "/hard/coded", null, true);
    }

    @Test
    public void testLegacyPathServices() throws Exception {
        getContainer().getRequiredService(AbstractPathService.pathNameOf("hardcoded"));
        ServiceName name1 = AbstractPathService.pathNameOf("add1");
        ServiceName name2 = AbstractPathService.pathNameOf("add2");
        ServiceName name3 = AbstractPathService.pathNameOf("add3");
        Assert.assertNull(getContainer().getService(name1));
        Assert.assertNull(getContainer().getService(name2));

        TestServiceListener listener = new TestServiceListener();
        getContainer().addListener(listener);

        listener.reset(1);
        ModelNode operation = createOperation(ADD);
        operation.get(OP_ADDR).add(PATH, "add1");
        operation.get(PATH).set("xyz");
        executeForResult(operation);
        listener.latch.await();
        Assert.assertEquals(new ExpectedResultBuilder().addUp(name1).map, listener.services);
        checkServiceAndPathEntry("add1", "xyz", null);

        listener.reset(1);
        operation = createOperation(ADD);
        operation.get(OP_ADDR).add(PATH, "add2");
        operation.get(PATH).set("abc");
        executeForResult(operation);
        listener.latch.await();
        Assert.assertEquals(new ExpectedResultBuilder().addUp(name2).map, listener.services);
        checkServiceAndPathEntry("add2", "abc", null);


        listener.reset(1);
        operation = createOperation(ADD);
        operation.get(OP_ADDR).add(PATH, "add3");
        operation.get(PATH).set("456");
        operation.get(RELATIVE_TO).set("add1");
        executeForResult(operation);
        listener.latch.await();
        ServiceController<?> original3 = getContainer().getRequiredService(name3);
        checkServiceAndPathEntry("add1", "xyz", null);
        checkServiceAndPathEntry("add2", "abc", null);
        checkServiceAndPathEntry("add3", "456", "add1");

        listener.reset(2);
        operation = createOperation(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).add(PATH, "add3");
        operation.get(NAME).set(PATH);
        operation.get(VALUE).set("new-value");
        executeForResult(operation);
        listener.latch.await();
        Assert.assertEquals(new ExpectedResultBuilder().addRemove(name3).addUp(name3).map, listener.services);
        checkServiceAndPathEntry("add3", "new-value", "add1");

        listener.reset(2);
        operation = createOperation(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).add(PATH, "add3");
        operation.get(NAME).set(RELATIVE_TO);
        operation.get(VALUE).set("add2");
        executeForResult(operation);
        listener.latch.await();
        Assert.assertEquals(new ExpectedResultBuilder().addRemove(name3).addUp(name3).map, listener.services);
        checkServiceAndPathEntry("add3", "new-value", "add2");

        listener.reset(2);
        operation = createOperation(UNDEFINE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).add(PATH, "add3");
        operation.get(NAME).set(RELATIVE_TO);
        executeForResult(operation);
        listener.latch.await();
        Assert.assertEquals(new ExpectedResultBuilder().addRemove(name3).addUp(name3).map, listener.services);
        checkServiceAndPathEntry("add3", "new-value", null);

        listener.reset(2);
        operation = createOperation(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).add(PATH, "add3");
        operation.get(NAME).set(PATH);
        operation.get(VALUE).set("newer-value");
        executeForResult(operation);
        listener.latch.await();
        Assert.assertEquals(new ExpectedResultBuilder().addRemove(name3).addUp(name3).map, listener.services);
        checkServiceAndPathEntry("add3", "newer-value", null);


        listener.reset(2);
        operation = createOperation(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).add(PATH, "add3");
        operation.get(NAME).set(RELATIVE_TO);
        operation.get(VALUE).set("add1");
        executeForResult(operation);
        listener.latch.await();
        Assert.assertEquals(new ExpectedResultBuilder().addRemove(name3).addUp(name3).map, listener.services);
        checkServiceAndPathEntry("add3", "newer-value", "add1");

        listener.reset(1);
        operation = createOperation(REMOVE);
        operation.get(OP_ADDR).add(PATH, "add3");
        executeForResult(operation);
        listener.latch.await();
        Assert.assertNull(getContainer().getService(name3));
        Assert.assertEquals(new ExpectedResultBuilder().addRemove(name3).map, listener.services);
    }

    private void checkServiceAndPathEntry(String name, String path, String relativeTo) {
        ServiceController<?> pathManagerService = getContainer().getRequiredService(PathManagerService.SERVICE_NAME);
        Assert.assertEquals(State.UP, pathManagerService.getState());
        PathManagerService pathManager = (PathManagerService)pathManagerService.getValue();

        ServiceController<?> pathService = getContainer().getRequiredService(AbstractPathService.pathNameOf(name));
        String servicePath = (String)pathService.getValue();

        PathEntry pathEntry = pathManager.getPathEntry(name);
        Assert.assertNotNull(pathEntry);

        Assert.assertEquals(name, pathEntry.getName());
        Assert.assertEquals(path, pathEntry.getPath());
        Assert.assertEquals(relativeTo, pathEntry.getRelativeTo());
        Assert.assertEquals(servicePath, pathEntry.resolvePath());
    }

    @Test
    public void testPathManagerAddNotifications() throws Exception {
        ServiceController<?> pathManagerService = getContainer().getRequiredService(PathManagerService.SERVICE_NAME);
        PathManager pathManager = (PathManager)pathManagerService.getValue();

        PerformChangeCallback allCallback1 = new PerformChangeCallback(pathManager, "add1", Event.ADDED, Event.REMOVED, Event.UPDATED);
        PerformChangeCallback addCallback1 = new PerformChangeCallback(pathManager, "add1", Event.ADDED);
        PerformChangeCallback changeCallback1 = new PerformChangeCallback(pathManager, "add1", Event.UPDATED);
        PerformChangeCallback removeCallback1 = new PerformChangeCallback(pathManager, "add1", Event.REMOVED);
        PerformChangeCallback allCallback2 = new PerformChangeCallback(pathManager, "add2", Event.ADDED, Event.REMOVED, Event.UPDATED);
        PerformChangeCallback allCallback3 = new PerformChangeCallback(pathManager, "add3", Event.ADDED, Event.REMOVED, Event.UPDATED);


        //Test callbacks for 1
        ModelNode operation = createOperation(ADD);
        operation.get(OP_ADDR).add(PATH, "add1");
        operation.get(PATH).set("xyz");
        executeForResult(operation);


        allCallback1.checkEvent(Event.ADDED, "add1", "xyz", null);
        allCallback1.checkDone();
        addCallback1.checkEvent(Event.ADDED, "add1", "xyz", null);
        addCallback1.checkDone();
        changeCallback1.checkDone();
        removeCallback1.checkDone();
        allCallback2.checkDone();
        allCallback3.checkDone();

        //Test callbacks for 2
        operation = createOperation(ADD);
        operation.get(OP_ADDR).add(PATH, "add2");
        operation.get(PATH).set("123");
        operation.get(RELATIVE_TO).set("add1");
        executeForResult(operation);

        allCallback1.checkDone();
        addCallback1.checkDone();
        changeCallback1.checkDone();
        removeCallback1.checkDone();
        allCallback2.checkEvent(Event.ADDED, "add2", "123", "add1");
        allCallback2.checkDone();
        allCallback3.checkDone();

        //Check the removal of the callback worked
        allCallback3.remove();
        operation = createOperation(ADD);
        operation.get(OP_ADDR).add(PATH, "add3");
        operation.get(PATH).set("abc");
        executeForResult(operation);

        allCallback1.checkDone();
        addCallback1.checkDone();
        changeCallback1.checkDone();
        removeCallback1.checkDone();
        allCallback2.checkDone();
        allCallback3.checkDone();
    }

    @Test
    public void testPathManagerRemoveNotifications() throws Exception {
        testAddPath();
        ServiceController<?> pathManagerService = getContainer().getRequiredService(PathManagerService.SERVICE_NAME);
        PathManager pathManager = (PathManager)pathManagerService.getValue();

        PerformChangeCallback allCallback1 = new PerformChangeCallback(pathManager, "add1", Event.ADDED, Event.REMOVED, Event.UPDATED);
        PerformChangeCallback addCallback1 = new PerformChangeCallback(pathManager, "add1", Event.ADDED);
        PerformChangeCallback changeCallback1 = new PerformChangeCallback(pathManager, "add1", Event.UPDATED);
        PerformChangeCallback removeCallback1 = new PerformChangeCallback(pathManager, "add1", Event.REMOVED);
        PerformChangeCallback allCallback2 = new PerformChangeCallback(pathManager, "add2", Event.ADDED, Event.REMOVED, Event.UPDATED);

        //Test callbacks for 2
        ModelNode operation = createOperation(REMOVE);
        operation.get(OP_ADDR).add(PATH, "add2");
        executeForResult(operation);

        allCallback1.checkDone();
        addCallback1.checkDone();
        changeCallback1.checkDone();
        removeCallback1.checkDone();
        allCallback2.checkEvent(Event.REMOVED, "add2", "123", "add1");
        allCallback2.checkDone();

        //Test callbacks for 1
        operation = createOperation(REMOVE);
        operation.get(OP_ADDR).add(PATH, "add1");
        executeForResult(operation);


        allCallback1.checkEvent(Event.REMOVED, "add1", "xyz", null);
        allCallback1.checkDone();
        addCallback1.checkDone();
        changeCallback1.checkDone();
        removeCallback1.checkEvent(Event.REMOVED, "add1", "xyz", null);
        removeCallback1.checkDone();
        allCallback2.checkDone();


        //Test that the removed callbacks don't get triggered
        allCallback1.remove();
        addCallback1.remove();
        changeCallback1.remove();
        removeCallback1.remove();
        allCallback2.remove();

        testAddPath();

        operation = createOperation(REMOVE);
        operation.get(OP_ADDR).add(PATH, "add2");
        executeForResult(operation);

        operation = createOperation(REMOVE);
        operation.get(OP_ADDR).add(PATH, "add1");
        executeForResult(operation);

        allCallback1.checkDone();
        addCallback1.checkDone();
        changeCallback1.checkDone();
        removeCallback1.checkDone();
        allCallback2.checkDone();

    }

    @Test
    public void testPathManagerChangeNotifications() throws Exception {
        testAddPath();
        ServiceController<?> pathManagerService = getContainer().getRequiredService(PathManagerService.SERVICE_NAME);
        PathManager pathManager = (PathManager)pathManagerService.getValue();

        ModelNode operation = createOperation(ADD);
        operation.get(OP_ADDR).add(PATH, "add3");
        operation.get(PATH).set("xyz");
        executeForResult(operation);

        PerformChangeCallback allCallback1 = new PerformChangeCallback(pathManager, "add1", Event.ADDED, Event.REMOVED, Event.UPDATED);
        PerformChangeCallback allCallback2 = new PerformChangeCallback(pathManager, "add2", Event.ADDED, Event.REMOVED, Event.UPDATED);
        PerformChangeCallback addCallback2 = new PerformChangeCallback(pathManager, "add2", Event.ADDED);
        PerformChangeCallback changeCallback2 = new PerformChangeCallback(pathManager, "add2", Event.UPDATED);
        PerformChangeCallback removeCallback2 = new PerformChangeCallback(pathManager, "add2", Event.REMOVED);

        //Test callbacks for 2 relative to absolute
        operation = createOperation(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).add(PATH, "add2");
        operation.get(NAME).set(RELATIVE_TO);
        operation.get(VALUE).set("add3");
        executeForResult(operation);

        allCallback1.checkDone();
        allCallback2.checkEvent(Event.UPDATED, "add2", "123", "add3");
        allCallback2.checkDone();
        addCallback2.checkDone();
        changeCallback2.checkEvent(Event.UPDATED, "add2", "123", "add3");
        changeCallback2.checkDone();
        removeCallback2.checkDone();

        operation = createOperation(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).add(PATH, "add2");
        operation.get(NAME).set(RELATIVE_TO);
        operation.get(VALUE).set(new ModelNode());
        executeForResult(operation);

        allCallback1.checkDone();
        allCallback2.checkEvent(Event.UPDATED, "add2", "123", null);
        allCallback2.checkDone();
        addCallback2.checkDone();
        changeCallback2.checkEvent(Event.UPDATED, "add2", "123", null);
        changeCallback2.checkDone();
        removeCallback2.checkDone();

        operation = createOperation(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).add(PATH, "add2");
        operation.get(NAME).set(PATH);
        operation.get(VALUE).set("abc");
        executeForResult(operation);

        allCallback1.checkDone();
        allCallback2.checkEvent(Event.UPDATED, "add2", "abc", null);
        allCallback2.checkDone();
        addCallback2.checkDone();
        changeCallback2.checkEvent(Event.UPDATED, "add2", "abc", null);
        changeCallback2.checkDone();
        removeCallback2.checkDone();

        //Test callbacks for 2 relative to absolute
        operation = createOperation(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).add(PATH, "add2");
        operation.get(NAME).set(RELATIVE_TO);
        operation.get(VALUE).set("add1");
        executeForResult(operation);

        allCallback1.checkDone();
        allCallback2.checkEvent(Event.UPDATED, "add2", "abc", "add1");
        allCallback2.checkDone();
        addCallback2.checkDone();
        changeCallback2.checkEvent(Event.UPDATED, "add2", "abc", "add1");
        changeCallback2.checkDone();
        removeCallback2.checkDone();
    }

    @Test
    public void testCannotRemoveDependentService() throws Exception {
        testAddPath();

        ServiceController<?> pathManagerService = getContainer().getRequiredService(PathManagerService.SERVICE_NAME);
        PathManager pathManager = (PathManager)pathManagerService.getValue();

        PerformChangeCallback allCallback1 = new PerformChangeCallback(pathManager, "add2", Event.ADDED, Event.REMOVED, Event.UPDATED);
        PerformChangeCallback allCallback2 = new PerformChangeCallback(pathManager, "add2", Event.ADDED, Event.REMOVED, Event.UPDATED);

        checkServiceAndPathEntry("add1", "xyz", null);
        checkServiceAndPathEntry("add2", "123", "add1");

        ModelNode operation = createOperation(REMOVE);
        operation.get(OP_ADDR).add(PATH, "add1");
        executeForFailure(operation);

        allCallback1.checkDone();
        allCallback2.checkDone();

        checkServiceAndPathEntry("add1", "xyz", null);
        checkServiceAndPathEntry("add2", "123", "add1");
    }

    @Test
    public void testBadAdd() throws Exception {
        ServiceController<?> pathManagerService = getContainer().getRequiredService(PathManagerService.SERVICE_NAME);
        PathManager pathManager = (PathManager)pathManagerService.getValue();

        PerformChangeCallback allCallback1 = new PerformChangeCallback(pathManager, "add1", Event.ADDED, Event.REMOVED, Event.UPDATED);

        ModelNode operation = createOperation(ADD);
        operation.get(OP_ADDR).add(PATH, "add1");
        operation.get(PATH).set("123");
        operation.get(RELATIVE_TO).set("bad");
        executeForFailure(operation);

        try {
            ServiceController<?> svc = getContainer().getRequiredService(AbstractPathService.pathNameOf("add1"));
            if (svc.getState() == State.UP) {
                Assert.fail("Should not managed to install service");
            }
        } catch (Exception expected) {
        }

        allCallback1.checkEvent(Event.ADDED, "add1", "123", "bad");
        allCallback1.checkEvent(Event.REMOVED, "add1", "123", "bad");
        allCallback1.checkDone();
    }

    @Test
    public void testBadChangeNoNotification() throws Exception {
        ServiceController<?> pathManagerService = getContainer().getRequiredService(PathManagerService.SERVICE_NAME);
        PathManager pathManager = (PathManager)pathManagerService.getValue();

        PerformChangeCallback allCallback1 = new PerformChangeCallback(pathManager, "add1", Event.ADDED, Event.REMOVED, Event.UPDATED);


        ModelNode operation = createOperation(ADD);
        operation.get(OP_ADDR).add(PATH, "add1");
        operation.get(PATH).set("123");
        executeForResult(operation);
        allCallback1.checkEvent(Event.ADDED, "add1", "123", null);
        allCallback1.checkDone();


        operation = createOperation(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).add(PATH, "add1");
        operation.get(NAME).set(RELATIVE_TO);
        operation.get(VALUE).set("bad");

        //TODO I changed this to fail, not 100% sure that is correct
        executeForFailure(operation);

        ServiceController<?> svc = getContainer().getRequiredService(AbstractPathService.pathNameOf("add1"));
        allCallback1.checkDone();
        checkServiceAndPathEntry("add1", "123", null);
    }

    @Test
    public void testChangeDependentServiceNotificationIsCascaded() throws Exception {
        testAddPath();

        ServiceController<?> pathManagerService = getContainer().getRequiredService(PathManagerService.SERVICE_NAME);
        PathManager pathManager = (PathManager)pathManagerService.getValue();

        PerformChangeCallback allCallback1 = new PerformChangeCallback(pathManager, "add1", Event.ADDED, Event.REMOVED, Event.UPDATED);
        PerformChangeCallback allCallback2 = new PerformChangeCallback(pathManager, "add2", Event.ADDED, Event.REMOVED, Event.UPDATED);
        PerformChangeCallback allCallback3 = new PerformChangeCallback(pathManager, "add3", Event.ADDED, Event.REMOVED, Event.UPDATED);

        checkServiceAndPathEntry("add1", "xyz", null);
        checkServiceAndPathEntry("add2", "123", "add1");

        ModelNode operation = createOperation(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).add(PATH, "add1");
        operation.get(NAME).set(PATH);
        operation.get(VALUE).set("abc");
        executeForResult(operation);
        checkServiceAndPathEntry("add1", "abc", null);
        checkServiceAndPathEntry("add2", "123", "add1");
        allCallback1.checkEvent(Event.UPDATED, "add1", "abc", null);
        allCallback1.checkDone();
        allCallback2.checkEvent(Event.UPDATED, "add2", "123", "add1");
        allCallback2.checkDone();
        allCallback3.checkDone();

        operation = createOperation(ADD);
        operation.get(OP_ADDR).add(PATH, "add3");
        operation.get(NAME).set(PATH);
        operation.get(PATH).set("def");
        operation.get(RELATIVE_TO).set("add2");
        executeForResult(operation);

        allCallback1.clear();
        allCallback2.clear();
        allCallback3.clear();

        operation = createOperation(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).add(PATH, "add1");
        operation.get(NAME).set(PATH);
        operation.get(VALUE).set("456");
        executeForResult(operation);
        checkServiceAndPathEntry("add1", "456", null);
        checkServiceAndPathEntry("add2", "123", "add1");
        checkServiceAndPathEntry("add3", "def", "add2");
        allCallback1.checkEvent(Event.UPDATED, "add1", "456", null);
        allCallback1.checkDone();
        allCallback2.checkEvent(Event.UPDATED, "add2", "123", "add1");
        allCallback2.checkDone();
        allCallback3.checkEvent(Event.UPDATED, "add3", "def", "add2");
        allCallback3.checkDone();


        operation = createOperation(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).add(PATH, "add3");
        operation.get(NAME).set(RELATIVE_TO);
        operation.get(VALUE).set(new ModelNode());
        executeForResult(operation);

        allCallback1.clear();
        allCallback2.clear();
        allCallback3.clear();

        operation = createOperation(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).add(PATH, "add1");
        operation.get(NAME).set(PATH);
        operation.get(VALUE).set("abc");
        executeForResult(operation);
        checkServiceAndPathEntry("add1", "abc", null);
        checkServiceAndPathEntry("add2", "123", "add1");
        checkServiceAndPathEntry("add3", "def", null);
        allCallback1.checkEvent(Event.UPDATED, "add1", "abc", null);
        allCallback1.checkDone();
        allCallback2.checkEvent(Event.UPDATED, "add2", "123", "add1");
        allCallback2.checkDone();
        allCallback3.checkDone();

        operation = createOperation(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).add(PATH, "add3");
        operation.get(NAME).set(RELATIVE_TO);
        operation.get(VALUE).set("add2");
        executeForResult(operation);

        allCallback1.clear();
        allCallback2.clear();
        allCallback3.clear();

        operation = createOperation(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).add(PATH, "add1");
        operation.get(NAME).set(PATH);
        operation.get(VALUE).set("456");
        executeForResult(operation);
        checkServiceAndPathEntry("add1", "456", null);
        checkServiceAndPathEntry("add2", "123", "add1");
        checkServiceAndPathEntry("add3", "def", "add2");
        allCallback1.checkEvent(Event.UPDATED, "add1", "456", null);
        allCallback1.checkDone();
        allCallback2.checkEvent(Event.UPDATED, "add2", "123", "add1");
        allCallback2.checkDone();
        allCallback3.checkEvent(Event.UPDATED, "add3", "def", "add2");
        allCallback3.checkDone();

        operation = createOperation(REMOVE);
        operation.get(OP_ADDR).add(PATH, "add3");
        operation.get(NAME).set(RELATIVE_TO);
        operation.get(VALUE).set("add2");
        executeForResult(operation);

        allCallback1.clear();
        allCallback2.clear();
        allCallback3.clear();

        operation = createOperation(WRITE_ATTRIBUTE_OPERATION);
        operation.get(OP_ADDR).add(PATH, "add1");
        operation.get(NAME).set(PATH);
        operation.get(VALUE).set("abc");
        executeForResult(operation);
        checkServiceAndPathEntry("add1", "abc", null);
        checkServiceAndPathEntry("add2", "123", "add1");
        allCallback1.checkEvent(Event.UPDATED, "add1", "abc", null);
        allCallback1.checkDone();
        allCallback2.checkEvent(Event.UPDATED, "add2", "123", "add1");
        allCallback2.checkDone();
        allCallback3.checkDone();
    }


    private void checkPath(ModelNode result, String pathName, String path, String relativeTo, boolean readOnly) {
        Assert.assertTrue(result.get(PATH).hasDefined(pathName));
        Assert.assertTrue(result.get(PATH, pathName).hasDefined(NAME));
        Assert.assertEquals(pathName, result.get(PATH, pathName, NAME).asString());
        Assert.assertTrue(result.get(PATH, pathName).hasDefined(PATH));
        Assert.assertEquals(path, result.get(PATH, pathName, PATH).asString());
        if (relativeTo == null) {
            Assert.assertFalse(result.get(PATH, pathName).hasDefined(RELATIVE_TO));
        } else {
            Assert.assertTrue(result.get(PATH, pathName).hasDefined(RELATIVE_TO));
            Assert.assertEquals(relativeTo, result.get(PATH, pathName, RELATIVE_TO).asString());
        }
        Assert.assertEquals(readOnly, result.get(PATH, pathName, READ_ONLY).asBoolean());
    }

    private ModelNode readResource() throws Exception {
        ModelNode operation = createOperation(READ_RESOURCE_OPERATION);
        operation.get(RECURSIVE).set(true);

        return executeForResult(operation);
    }

    @Override
    protected DescriptionProvider getRootDescriptionProvider() {
        return new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode node = new ModelNode();
                node.get(DESCRIPTION).set("The root node of the test management API");
                node.get(CHILDREN, PATH, DESCRIPTION).set("A list of paths");
                node.get(CHILDREN, PATH, MIN_OCCURS).set(0);
                node.get(CHILDREN, PATH, MODEL_DESCRIPTION);
                return node;
            }
        };
    }

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration registration) {
        pathManagerService = new PathManagerService() {
            {
                super.addHardcodedAbsolutePath(getContainer(), "hardcoded", "/hard/coded");
            }
        };
        registration.registerOperationHandler(READ_RESOURCE_OPERATION, GlobalOperationHandlers.READ_RESOURCE, CommonProviders.READ_RESOURCE_PROVIDER, true, EnumSet.of(Flag.RUNTIME_ONLY));
        registration.registerOperationHandler(READ_ATTRIBUTE_OPERATION, GlobalOperationHandlers.READ_ATTRIBUTE, CommonProviders.READ_ATTRIBUTE_PROVIDER, true, EnumSet.of(Flag.RUNTIME_ONLY));
        registration.registerOperationHandler(READ_RESOURCE_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_RESOURCE_DESCRIPTION, CommonProviders.READ_RESOURCE_DESCRIPTION_PROVIDER, true, EnumSet.of(Flag.RUNTIME_ONLY));
        registration.registerOperationHandler(READ_CHILDREN_NAMES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_NAMES, CommonProviders.READ_CHILDREN_NAMES_PROVIDER, true, EnumSet.of(Flag.RUNTIME_ONLY));
        registration.registerOperationHandler(READ_CHILDREN_TYPES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_TYPES, CommonProviders.READ_CHILDREN_TYPES_PROVIDER, true, EnumSet.of(Flag.RUNTIME_ONLY));
        registration.registerOperationHandler(READ_CHILDREN_RESOURCES_OPERATION, GlobalOperationHandlers.READ_CHILDREN_RESOURCES, CommonProviders.READ_CHILDREN_RESOURCES_PROVIDER, true, EnumSet.of(Flag.RUNTIME_ONLY));
        registration.registerOperationHandler(READ_OPERATION_NAMES_OPERATION, GlobalOperationHandlers.READ_OPERATION_NAMES, CommonProviders.READ_OPERATION_NAMES_PROVIDER, true, EnumSet.of(Flag.RUNTIME_ONLY));
        registration.registerOperationHandler(READ_OPERATION_DESCRIPTION_OPERATION, GlobalOperationHandlers.READ_OPERATION_DESCRIPTION, CommonProviders.READ_OPERATION_PROVIDER, true, EnumSet.of(Flag.RUNTIME_ONLY));
        registration.registerOperationHandler(UNDEFINE_ATTRIBUTE_OPERATION, GlobalOperationHandlers.UNDEFINE_ATTRIBUTE, CommonProviders.UNDEFINE_ATTRIBUTE_PROVIDER, true);
        registration.registerOperationHandler(WRITE_ATTRIBUTE_OPERATION, GlobalOperationHandlers.WRITE_ATTRIBUTE, CommonProviders.WRITE_ATTRIBUTE_PROVIDER, true);

        TestServiceListener listener = new TestServiceListener();
        listener.reset(1);
        getContainer().addService(PathManagerService.SERVICE_NAME, pathManagerService)
                .addListener(listener)
                .install();

        try {
            listener.latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }

        registration.registerSubModel(PathResourceDefinition.createSpecified(pathManagerService));

        pathManagerService.addPathManagerResources(rootResource);
    }

    private class TestServiceListener extends AbstractServiceListener<Object>{

        volatile CountDownLatch latch;
        Map<Transition, ServiceName> services = Collections.synchronizedMap(new LinkedHashMap<ServiceController.Transition, ServiceName>());


        void reset(int count) {
            latch = new CountDownLatch(count);
            services.clear();
        }

        public void transition(ServiceController<? extends Object> controller, Transition transition) {
            if (transition == Transition.STARTING_to_UP || transition == Transition.REMOVING_to_REMOVED) {
                services.put(transition, controller.getName());
                latch.countDown();
            }
        }
    }

    private static class ExpectedResultBuilder {
        LinkedHashMap<Transition, ServiceName> map = new LinkedHashMap<ServiceController.Transition, ServiceName>();

        ExpectedResultBuilder addUp(ServiceName name){
            map.put(Transition.STARTING_to_UP, name);
            return this;
        }

        ExpectedResultBuilder addRemove(ServiceName name){
            map.put(Transition.REMOVING_to_REMOVED, name);
            return this;
        }
    }

    private static class PerformChangeCallback implements PathManager.Callback {

        private LinkedHashMap<Event, PathEntry> paths = new LinkedHashMap<PathManager.Event, PathEntry>();
        private Handle handle;

        PerformChangeCallback(PathManager pathManager, String pathName, Event...events) {
            if (handle != null) {
                throw new IllegalStateException("Already registered");
            }
            handle = pathManager.registerCallback(pathName, this, events);
        }

        void remove() {
            handle.remove();
            handle = null;
        }

        void clear() {
            paths.clear();
        }

        @Override
        public void pathEvent(Event event, PathEntry pathEntry) {
            paths.put(event, pathEntry);
        }

        void checkEvent(Event event, String name, String path, String relativeTo) {
            Iterator<Map.Entry<Event, PathEntry>> it = paths.entrySet().iterator();
            Assert.assertTrue(it.hasNext());
            Map.Entry<Event, PathEntry> entry = it.next();
            Assert.assertEquals(event, entry.getKey());
            PathEntry pathEntry = entry.getValue();
            Assert.assertEquals(name, pathEntry.getName());
            Assert.assertEquals(path, pathEntry.getPath());
            Assert.assertEquals(relativeTo, pathEntry.getRelativeTo());
            it.remove();
        }

        void checkDone() {
            Assert.assertTrue(paths.isEmpty());
        }

        @Override
        public void pathModelEvent(PathEventContext eventContext, String name) {
        }
    }
 }
