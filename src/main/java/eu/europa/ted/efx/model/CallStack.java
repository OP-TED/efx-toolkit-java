package eu.europa.ted.efx.model;

import java.util.InputMismatchException;
import java.util.Stack;

public class CallStack extends Stack<StackObjectBase> {

    public CallStack() {}

    public synchronized <T extends StackObjectBase> T pop(Class<T> expectedType) {
        Class<? extends StackObjectBase> actualType = peek().getClass();
        if (!expectedType.isAssignableFrom(actualType)) {
            throw new InputMismatchException(
                    "Type mistmatch. Expected " + expectedType.getSimpleName() + " instead of "
                            + this.peek().getClass().getSimpleName());
        }
        return expectedType.cast(this.pop());
    }
}
