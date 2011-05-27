package org.jboss.as.cli.operation.parsing;

import org.jboss.as.cli.CommandFormatException;

public final class OperationNameState extends DefaultParsingState {

    public static final String ID = "OP_NAME";
    public static final OperationNameState INSTANCE = new OperationNameState();

    public OperationNameState() {
        this(PropertyListState.INSTANCE);
    }

    public OperationNameState(final PropertyListState propList) {
        super(ID);
        setEnterHandler(GlobalCharacterHandlers.CONTENT_CHARACTER_HANDLER);
        setDefaultHandler(GlobalCharacterHandlers.CONTENT_CHARACTER_HANDLER);
        putHandler('(', new CharacterHandler(){
            @Override
            public void handle(ParsingContext ctx)
                    throws CommandFormatException {
                ctx.leaveState();
                ctx.enterState(propList);
            }});
    }
}