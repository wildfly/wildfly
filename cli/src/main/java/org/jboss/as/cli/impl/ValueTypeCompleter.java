/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.cli.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.cli.CommandContext;
import org.jboss.as.cli.CommandFormatException;
import org.jboss.as.cli.CommandLineCompleter;
import org.jboss.as.cli.Util;
import org.jboss.as.cli.parsing.CharacterHandler;
import org.jboss.as.cli.parsing.DefaultParsingState;
import org.jboss.as.cli.parsing.GlobalCharacterHandlers;
import org.jboss.as.cli.parsing.ParsingContext;
import org.jboss.as.cli.parsing.ParsingStateCallbackHandler;
import org.jboss.as.cli.parsing.StateParser;
import org.jboss.as.cli.parsing.WordCharacterHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Alexey Loubyansky
 *
 */
public class ValueTypeCompleter implements CommandLineCompleter {

    private static final List<ModelNode> BOOLEAN_LIST = new ArrayList<ModelNode>(2);
    static {
        BOOLEAN_LIST.add(new ModelNode(Boolean.FALSE));
        BOOLEAN_LIST.add(new ModelNode(Boolean.TRUE));
    }

    private final ModelNode propDescr;

    public ValueTypeCompleter(ModelNode propDescr) {
        if(propDescr == null || !propDescr.isDefined()) {
            throw new IllegalArgumentException("property description is null or undefined.");
        }
        this.propDescr = propDescr;
    }

    @Override
    public int complete(CommandContext ctx, String buffer, int cursor, List<String> candidates) {

/*        int nextCharIndex = 0;
        while (nextCharIndex < buffer.length()) {
            if (!Character.isWhitespace(buffer.charAt(nextCharIndex))) {
                break;
            }
            ++nextCharIndex;
        }
*/
        final ValueTypeCallbackHandler handler;
        try {
            handler = parse(buffer);
        } catch (CommandFormatException e) {
            // TODO add logging here
            return -1;
        }
        final Collection<String> foundCandidates = handler.getCandidates(propDescr);
        if(foundCandidates.isEmpty()) {
            return -1;
        }
        candidates.addAll(foundCandidates);
        return handler.getCompletionIndex();
    }

    protected ValueTypeCallbackHandler parse(String line) throws CommandFormatException {
        final ValueTypeCallbackHandler valueTypeHandler = new ValueTypeCallbackHandler(false);
        StateParser.parse(line, valueTypeHandler, InitialValueState.INSTANCE);
        return valueTypeHandler;
    }

    private final class ValueTypeCallbackHandler implements ParsingStateCallbackHandler {

        private static final String offsetStep = "  ";

        private final boolean logging;
        private int offset;

        private StringBuilder propBuf = new StringBuilder();

        private String prop;
        private List<String> propStack;
        private List<List<String>> mentionedPropStack = new ArrayList<List<String>>(2);
        private String lastEnteredState;
        private int lastStateIndex;
        private char lastStateChar;

//        ValueTypeCallbackHandler() {
//            this(false);
//        }

        ValueTypeCallbackHandler(boolean logging) {
            this.logging = logging;
        }

        public int getCompletionIndex() {
            //System.out.println("getCompletionIndex: " + lastStateChar + " " + lastStateIndex);
            switch(lastStateChar) {
                case '{':
                case '}':
                case '[':
                case ']':
                case '=':
                case ',':
                    return lastStateIndex + 1;
            }
            return lastStateIndex;
        }

        public Collection<String> getCandidates(ModelNode propDescr) {
            if(propDescr == null || !propDescr.isDefined()) {
                return Collections.emptyList();
            }
            if(!propDescr.has(Util.VALUE_TYPE)) {
                return Collections.emptyList();
            }
            ModelNode propType = propDescr.get(Util.VALUE_TYPE);
            int mentionedIndex = 0;
            //System.out.println("\n" + propStack + " prop=" + prop + " buf=" + propBuf + " lastState=" + lastEnteredState);
            if(propStack != null && !propStack.isEmpty()) {
                mentionedIndex = propStack.size();
                for(int i = 0; i < propStack.size(); ++i) {
                    final String propName = propStack.get(i);
                    if(!propType.has(propName)) {
                        return Collections.emptyList();
                    }
                    final ModelNode propNode = propType.get(propName);
                    if(propNode.has(Util.VALUE_TYPE)) {
                        propType = propNode.get(Util.VALUE_TYPE);
                        if(!propType.isDefined()) {
                            return Collections.emptyList();
                        }
                    } else {
                        return Collections.emptyList();
                    }
                }
            }

            if(prop == null) {
                if((PropertyState.ID.equals(lastEnteredState) || EqualsState.ID.equals(lastEnteredState)) && propBuf.length() > 0) {
                    prop = propBuf.toString();
                    propBuf.setLength(0);
                } else {
                    if(lastEnteredState == null) {
                        if(propDescr.has(Util.TYPE)) {
                            final ModelType type = propDescr.get(Util.TYPE).asType();
                            if(type.equals(ModelType.OBJECT)) {
                                return Collections.singletonList("{");
                            } else if(type.equals(ModelType.LIST)) {
                                return Collections.singletonList("[");
                            }
                        }
                    }
                    final List<String> mentionedProps = getMentionedProps(mentionedIndex);
                    if (mentionedProps == null || mentionedProps.isEmpty() || lastEnteredState.equals(ListItemSeparatorState.ID)) {
                        final List<String> candidates = new ArrayList<String>(propType.keys());
                        if (mentionedProps != null) {
                            candidates.removeAll(mentionedProps);
                        }
                        Collections.sort(candidates);
                        return candidates;
                    } else {
                        return Collections.emptyList();
                    }
                }
            }

            if(TextState.ID.equals(lastEnteredState)) {
                if(!propType.has(prop)) {
                    return Collections.emptyList();
                }
                propType = propType.get(prop);
                final List<ModelNode> allowed;
                if(!propType.has(Util.ALLOWED)) {
                    if(isBoolean(propType)) {
                        allowed = BOOLEAN_LIST;
                    } else {
                        return Collections.<String>emptyList();
                    }
                } else {
                    allowed = propType.get(Util.ALLOWED).asList();
                }
                final List<String> candidates = new ArrayList<String>();
                if(propBuf.length() > 0) {
                    final String value = propBuf.toString();
                    for (ModelNode candidate : allowed) {
                        final String candidateStr = candidate.asString();
                        if(candidateStr.startsWith(value)) {
                            candidates.add(candidateStr);
                        }
                    }
                } else {
                    for (ModelNode candidate : allowed) {
                        candidates.add(candidate.asString());
                    }
                }
                Collections.sort(candidates);
                return candidates;
            }

            final List<String> candidates;
            if(EqualsState.ID.equals(lastEnteredState)) {
                final List<ModelNode> allowed;
                if(isBoolean(propType)) {
                    allowed = BOOLEAN_LIST;
                } else {
                if(!propType.has(prop)) {
                    return Collections.emptyList();
                }
                propType = propType.get(prop);
                if(!propType.has(Util.ALLOWED)) {
                    if(propType.has(Util.VALUE_TYPE)) {
                        final ModelNode propValueType = propType.get(Util.VALUE_TYPE);
                        try {
                            propValueType.asType();
                            return Collections.emptyList();
                        } catch(IllegalArgumentException e) {
                            if(propType.has(Util.TYPE)) {
                                final ModelType type = propType.get(Util.TYPE).asType();
                                if(type.equals(ModelType.OBJECT)) {
                                    return Collections.singletonList("{");
                                } else if(type.equals(ModelType.LIST)) {
                                    return Collections.singletonList("[");
                                }
                            }
                        }
                    }
                    if(isBoolean(propType)) {
                        allowed = BOOLEAN_LIST;
                    } else {
                        return Collections.<String>emptyList();
                    }
                } else {
                    allowed = propType.get(Util.ALLOWED).asList();
                }
                }
                candidates = new ArrayList<String>();
                for(ModelNode candidate : allowed) {
                    candidates.add(candidate.asString());
                }
            } else if(StartObjectState.ID.equals(lastEnteredState) || StartListState.ID.equals(lastEnteredState)) {
                if(!propType.has(prop)) {
                    return Collections.emptyList();
                }
                propType = propType.get(prop);
                if(!propType.has(Util.VALUE_TYPE)) {
                    return Collections.emptyList();
                }
                final ModelNode propValueType = propType.get(Util.VALUE_TYPE);
                try {
                    propValueType.asType();
                    return Collections.emptyList();
                } catch(IllegalArgumentException e) {
                }
                candidates = new ArrayList<String>(propValueType.keys());
            } else {
                if(propBuf.length() > 0) {
                    if(!propType.has(prop)) {
                        return Collections.emptyList();
                    }
                    final ModelNode propNode = propType.get(prop);
                    if(propNode.has(Util.VALUE_TYPE)) {
                        propType = propNode.get(Util.VALUE_TYPE);
                    } else {
                        return Collections.emptyList();
                    }
                    prop = propBuf.toString();
                    ++mentionedIndex;
                } else if(ListItemSeparatorState.ID.equals(lastEnteredState)) {
                    if(!propType.has(prop)) {
                        return Collections.emptyList();
                    }
                    final ModelNode propNode = propType.get(prop);
                    if(propNode.has(Util.VALUE_TYPE)) {
                        propType = propNode.get(Util.VALUE_TYPE);
                    } else {
                        return Collections.emptyList();
                    }
                    prop = null;
                    ++mentionedIndex;
                }
                candidates = new ArrayList<String>();
                final List<String> mentionedProps = getMentionedProps(mentionedIndex);
                for (String candidate : propType.keys()) {
                    if (prop == null || candidate.startsWith(prop)) {
                        if(mentionedProps == null) {
                            candidates.add(candidate);
                        } else if(!mentionedProps.contains(candidate)) {
                            candidates.add(candidate);
                        }
                    }
                }
            }
            Collections.sort(candidates);
            return candidates;
        }

        protected boolean isBoolean(ModelNode propType) {
            return propType.has(Util.TYPE) && propType.get(Util.TYPE).asType().equals(ModelType.BOOLEAN);
        }

        protected List<String> getMentionedProps(int i) {
            List<String> mentionedProps = null;
            if(mentionedPropStack != null && i < mentionedPropStack.size()) {
                return mentionedPropStack.get(i);
/*                if(propStack == null) {
                    if(mentionedPropStack.size() == 1) {
                        mentionedProps = mentionedPropStack.get(0);
                    } else if(mentionedPropStack.size() > 1) {
                        throw new IllegalStateException();
                    }
                } else {
                    if(mentionedPropStack.size() != propStack.size() + 1) {
                        throw new IllegalStateException(mentionedPropStack.size() + " " + (propStack.size() + 1) + " " + mentionedPropStack);
                    }
                    mentionedProps = mentionedPropStack.get(propStack.size());
                }
*/            }
            return mentionedProps;
        }

        @Override
        public void enteredState(ParsingContext ctx) throws CommandFormatException {
            lastEnteredState = ctx.getState().getId();
            lastStateIndex = ctx.getLocation();
            lastStateChar = ctx.getCharacter();

            if(logging) {
                final StringBuilder buf = new StringBuilder();
                for (int i = 0; i < offset; ++i) {
                    buf.append(offsetStep);
                }
                buf.append("entered '" + lastStateChar + "' " + lastEnteredState);
                System.out.println(buf.toString());
                if(lastEnteredState.equals(PropertyListState.ID)) {
                    ++offset;
                }
            }

            if(lastEnteredState.equals(EqualsState.ID)) {
                if(prop != null) {
                    if(propStack == null) {
                        propStack = new ArrayList<String>();
                    }
                    propStack.add(prop);
                }
                prop = propBuf.toString();
//                enteredProperty(prop);
                propBuf.setLength(0);
            }
        }

        @Override
        public void leavingState(ParsingContext ctx) throws CommandFormatException {
            final String id = ctx.getState().getId();

            if (logging) {
                if (id.equals(PropertyListState.ID)) {
                    --offset;
                }
                final StringBuilder buf = new StringBuilder();
                for (int i = 0; i < offset; ++i) {
                    buf.append(offsetStep);
                }
                buf.append("leaving '" + ctx.getCharacter() + "' " + id);
                System.out.println(buf.toString());
            }

            if(ctx.isEndOfContent()) {
                return;
            }

            if(id.equals(TextState.ID)) {
                propBuf.setLength(0);
            } else if(id.equals(PropertyState.ID)) {
                if (propStack != null && propStack.size() > 0) {
                    final int propStackSize = propStack.size();
                    final String mentioned = prop;
                    if (propStackSize == 0) {
                        prop = null;
                    } else {
                        prop = propStack.remove(propStackSize - 1);
                    }
                    if(mentionedPropStack.size() < propStackSize + 1) {
                        final List<List<String>> tmp = mentionedPropStack;
                        mentionedPropStack = new ArrayList<List<String>>(propStackSize + 1);
                        mentionedPropStack.addAll(tmp);
                        for(int i = mentionedPropStack.size(); i <= propStackSize; ++i) {
                            mentionedPropStack.add(null);
                        }
                    } else if(mentionedPropStack.size() > propStackSize + 1) {
                        mentionedPropStack.set(propStackSize + 1, null);
                    }
                    List<String> mentionedProps = mentionedPropStack.get(propStackSize);
                    if(mentionedProps == null) {
                        mentionedProps = Collections.singletonList(mentioned);
                        mentionedPropStack.set(propStackSize, mentionedProps);
                    } else if(mentionedProps.size() == 1) {
                        List<String> tmp = mentionedProps;
                        mentionedProps = new ArrayList<String>();
                        mentionedProps.add(tmp.get(0));
                        mentionedProps.add(mentioned);
                        mentionedPropStack.set(propStackSize, mentionedProps);
                    } else {
                        mentionedProps.add(mentioned);
                    }
                } else {
                    if(mentionedPropStack.size() == 0) {
                        mentionedPropStack = new ArrayList<List<String>>(1);
                        mentionedPropStack.add(null);
                    }
                    List<String> mentionedProps = mentionedPropStack.get(0);
                    if(mentionedProps == null) {
                        mentionedProps = Collections.singletonList(prop);
                        mentionedPropStack.set(0, mentionedProps);
                    } else if(mentionedProps.size() == 1) {
                        List<String> tmp = mentionedProps;
                        mentionedProps = new ArrayList<String>();
                        mentionedProps.add(tmp.get(0));
                        mentionedProps.add(prop);
                        mentionedPropStack.set(0, mentionedProps);
                    } else {
                        mentionedProps.add(prop);
                    }

                    prop = null;
                }
            }
        }

        @Override
        public void character(ParsingContext ctx) throws CommandFormatException {
            final String id = ctx.getState().getId();

            if(logging) {
                final StringBuilder buf = new StringBuilder();
                for (int i = 0; i < offset; ++i) {
                    buf.append(offsetStep);
                }
                buf.append("char '" + ctx.getCharacter() + "' " + id);
                System.out.println(buf.toString());
            }

            if(id.equals(PropertyState.ID)) {
                final char ch = ctx.getCharacter();
                if(ch != '"' && !Character.isWhitespace(ch)) {
                    propBuf.append(ch);
                }
            } else if(id.equals(TextState.ID)) {
                propBuf.append(ctx.getCharacter());
            }
        }
    }

/*    private final class EchoCallbackHandler implements ParsingStateCallbackHandler {

        private int offset = 0;
        private String offsetStep = "    ";
        private final StringBuilder parsingBuf;

        private EchoCallbackHandler(StringBuilder parsingBuf) {
            this.parsingBuf = parsingBuf;
        }

        @Override
        public void enteredState(ParsingContext ctx) throws CommandFormatException {
            final String id = ctx.getState().getId();

                final StringBuilder buf = new StringBuilder();
            for(int i = 0; i < offset; ++i) {
                buf.append(offsetStep);
            }
            buf.append("entered '" + ctx.getCharacter() + "' " + id);
            System.out.println(buf.toString());

            if(id.equals(PropertyState.ID)) {
                for(int i = 0; i < offset; ++i) {
                    parsingBuf.append(offsetStep);
                }
            } else if(id.equals(EqualsState.ID)) {
                parsingBuf.append('=');
            } else if(id.equals(PropertyListState.ID)) {
                ++offset;
            } else if(id.equals(StartObjectState.ID)) {
                parsingBuf.append('{').append(Util.LINE_SEPARATOR);
            } else if(id.equals(StartListState.ID)) {
                parsingBuf.append('[').append(Util.LINE_SEPARATOR);
            }
        }

        @Override
        public void leavingState(ParsingContext ctx) throws CommandFormatException {
            final String id = ctx.getState().getId();

            if(id.equals(PropertyListState.ID)) {
                --offset;
            }

                final StringBuilder buf = new StringBuilder();
            for(int i = 0; i < offset; ++i) {
                buf.append(offsetStep);
            }
            buf.append("leaving '" + ctx.getCharacter() + "' " + id);
            System.out.println(buf.toString());

            if(id.equals(ListItemSeparatorState.ID)) {
                parsingBuf.append(",");
                parsingBuf.append(Util.LINE_SEPARATOR);
            } else if(id.equals(StartObjectState.ID)) {
                parsingBuf.append(Util.LINE_SEPARATOR);
                for(int i = 0; i < offset; ++i) {
                    parsingBuf.append(offsetStep);
                }
                parsingBuf.append('}');
            } else if(id.equals(StartListState.ID)) {
                parsingBuf.append(Util.LINE_SEPARATOR);
                for(int i = 0; i < offset; ++i) {
                    parsingBuf.append(offsetStep);
                }
                parsingBuf.append(']');
            }
        }

        @Override
        public void character(ParsingContext ctx) throws CommandFormatException {
            final String id = ctx.getState().getId();

                final StringBuilder buf = new StringBuilder();
            for(int i = 0; i < offset; ++i) {
                buf.append(offsetStep);
            }
            buf.append("char '" + ctx.getCharacter() + "' " + id);
            System.out.println(buf.toString());

            if(id.equals(PropertyState.ID) || id.equals(TextState.ID)) {
                parsingBuf.append(ctx.getCharacter());
            }
        }
    }
*/
    public interface ValueTypeCandidatesProvider {
        Collection<String> getCandidates(String chunk);
    }

    abstract static class ValueTypeCandidatesState extends DefaultParsingState implements ValueTypeCandidatesProvider {

        private final Collection<String> candidates = new ArrayList<String>();

        ValueTypeCandidatesState(String id) {
            super(id);
        }

        protected void addCandidate(String candidate) {
            candidates.add(candidate);
        }
        protected void addCandidates(Collection<String> candidates) {
            this.candidates.addAll(candidates);
        }

        @Override
        public Collection<String> getCandidates(String chunk) {
            if(candidates.isEmpty()) {
                return Collections.emptyList();
            }
            if(chunk == null || chunk.length() == 0) {
                return candidates;
            }
            final List<String> filtered = new ArrayList<String>(candidates.size());
            for(String candidate : candidates) {
                if(candidate.startsWith(chunk)) {
                    filtered.add(candidate);
                }
            }
            return filtered;
        }
    }

    public static class InitialValueState extends ValueTypeCandidatesState {
        public static final String ID = "INITVAL";

        public static final InitialValueState INSTANCE = new InitialValueState();

        public InitialValueState() {
            this(PropertyState.INSTANCE);
        }

        public InitialValueState(final PropertyState prop) {
            super(ID);
            enterState('{', PropertyListState.INSTANCE);
            enterState('[', PropertyListState.INSTANCE);
            setDefaultHandler(new CharacterHandler() {
                @Override
                public void handle(ParsingContext ctx) throws CommandFormatException {
                    //ctx.enterState(prop);
                    ctx.enterState(PropertyListState.INSTANCE);
                }});
            addCandidate("{");
            addCandidate("[");
            addCandidates(prop.getCandidates(null));
        }
    }

    public static class StartObjectState extends DefaultParsingState {
        public static final String ID = "OBJ";

        private static StartObjectState INSTANCE = new StartObjectState();

        public StartObjectState() {
            super(ID);
            setDefaultHandler(new CharacterHandler(){
                @Override
                public void handle(ParsingContext ctx) throws CommandFormatException {
                    ctx.enterState(PropertyListState.INSTANCE);
                }});
            setIgnoreWhitespaces(true);
            setReturnHandler(new CharacterHandler(){
                @Override
                public void handle(ParsingContext ctx) throws CommandFormatException {
                    ctx.leaveState();
                }});
        }
    }

    public static class StartListState extends DefaultParsingState {
        public static final String ID = "LST";

        private static StartListState INSTANCE = new StartListState();

        public StartListState() {
            super(ID);
            setDefaultHandler(new CharacterHandler(){
                @Override
                public void handle(ParsingContext ctx) throws CommandFormatException {
                    ctx.enterState(PropertyListState.INSTANCE);
                }});
            setIgnoreWhitespaces(true);
            setReturnHandler(new CharacterHandler(){
                @Override
                public void handle(ParsingContext ctx) throws CommandFormatException {
                    if(!ctx.isEndOfContent()) {
                        ctx.advanceLocation(1);
                    }
                    ctx.leaveState();
                }});
        }
    }

    public static class PropertyListState extends DefaultParsingState {
        public static final String ID = "PROPLIST";

        public static final PropertyListState INSTANCE = new PropertyListState();

        public PropertyListState() {
            super(ID);
            setEnterHandler(new CharacterHandler() {
                @Override
                public void handle(ParsingContext ctx) throws CommandFormatException {
                    ctx.enterState(PropertyState.INSTANCE);
                }});
            setDefaultHandler(new CharacterHandler(){
                @Override
                public void handle(ParsingContext ctx) throws CommandFormatException {
                    ctx.enterState(PropertyState.INSTANCE);
                }});
            setReturnHandler(new CharacterHandler(){
                @Override
                public void handle(ParsingContext ctx) throws CommandFormatException {
                    if(ctx.isEndOfContent()) {
                        ctx.leaveState();
                        return;
                    }
                    final char ch = ctx.getCharacter();
                    if (ch == '}' || ch == ']') {
                        if(ctx.getLocation() < ctx.getInput().length() - 1) {
                            ctx.advanceLocation(1);
                        }
                        ctx.leaveState();
                    } else {
                        getHandler(ch).handle(ctx);
                    }
                }});
            enterState(',', ListItemSeparatorState.INSTANCE);
            setIgnoreWhitespaces(true);
        }
    }

    public static class ListItemSeparatorState extends DefaultParsingState implements ValueTypeCandidatesProvider {
        public static final String ID = "ITMSEP";

        public static final ListItemSeparatorState INSTANCE = new ListItemSeparatorState();

        public ListItemSeparatorState() {
            super(ID);
            setEnterHandler(new CharacterHandler(){
                @Override
                public void handle(ParsingContext ctx) throws CommandFormatException {
                    if(!ctx.isEndOfContent()) {
                        ctx.advanceLocation(1);
                    }
                    ctx.leaveState();
                }});
        }

        @Override
        public Collection<String> getCandidates(String chunk) {
            return Collections.emptyList();
        }
    }

    public static class PropertyState extends DefaultParsingState implements ValueTypeCandidatesProvider {
        public static final String ID = "PROP";

        public static final PropertyState INSTANCE = new PropertyState();

        private final Collection<String> candidates = new ArrayList<String>(2);

        public PropertyState() {
            super(ID);
            setHandleEntrance(true);
            putHandler('{', GlobalCharacterHandlers.NOOP_CHARACTER_HANDLER);
            putHandler('[', GlobalCharacterHandlers.NOOP_CHARACTER_HANDLER);
            setDefaultHandler(WordCharacterHandler.IGNORE_LB_ESCAPE_ON);
            enterState('=', EqualsState.INSTANCE);
            setReturnHandler(new CharacterHandler(){
                @Override
                public void handle(ParsingContext ctx) throws CommandFormatException {
                    ctx.leaveState();
                }});
            leaveState(',');
            leaveState(']');
            leaveState('}');
            candidates.add("=");
        }

        @Override
        public Collection<String> getCandidates(String chunk) {
            // TODO and on the '=' I should add value candidates
            return candidates;
        }
    }

    public static class EqualsState extends ValueTypeCandidatesState {

        public static final String ID = "EQ";

        public static final EqualsState INSTANCE = new EqualsState();

        public EqualsState() {
            super(ID);
            setIgnoreWhitespaces(true);
            setDefaultHandler(new CharacterHandler() {
                @Override
                public void handle(ParsingContext ctx) throws CommandFormatException {
                    ctx.enterState(TextState.INSTANCE);
                }});
            putHandler('>', GlobalCharacterHandlers.NOOP_CHARACTER_HANDLER);
            enterState('{', StartObjectState.INSTANCE);
            enterState('[', StartListState.INSTANCE);
            addCandidate("{");
            addCandidate("[");
            setReturnHandler(GlobalCharacterHandlers.LEAVE_STATE_HANDLER);
        }
    }

    public static class TextState extends ValueTypeCandidatesState {

        public static final String ID = "TEXT";

        public static final TextState INSTANCE = new TextState();

        public TextState() {
            super(ID);
            setHandleEntrance(true);
            setDefaultHandler(WordCharacterHandler.IGNORE_LB_ESCAPE_ON);
            leaveState(',');
            leaveState('=');
            leaveState('}');
            leaveState(']');
        }
    }
}
