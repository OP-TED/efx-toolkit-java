package eu.europa.ted.efx.model;

import java.util.Stack;
import org.antlr.v4.runtime.misc.ParseCancellationException;

public class CallStack extends Stack<CallStackObjectBase> {

    public CallStack() {}

    public synchronized <T extends CallStackObjectBase> T pop(Class<T> expectedType) {
        Class<? extends CallStackObjectBase> actualType = peek().getClass();
        if (!expectedType.isAssignableFrom(actualType)) {
            throw new ParseCancellationException(
                    "Type mismatch. Expected " + expectedType.getSimpleName() + " instead of "
                            + this.peek().getClass().getSimpleName());
        }
        return expectedType.cast(this.pop());
    }
}
