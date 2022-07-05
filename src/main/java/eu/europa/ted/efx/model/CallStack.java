package eu.europa.ted.efx.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import org.antlr.v4.runtime.misc.ParseCancellationException;

public class CallStack extends Stack<CallStackObjectBase> {

    Map<String, Class<? extends Expression>> variables = new HashMap<String, Class<? extends Expression>>();

    public CallStack() {}

    public void pushVariable(Expression item) {
        if (this.variables.containsKey(item.script)) {
            this.push(Expression.instantiate(item.script, variables.get(item.script)));
        } else if (item.getClass() == Expression.class) {
            throw new ParseCancellationException();
        } else {
            this.variables.put(item.script, item.getClass());
            this.push(item);
        }
    }

    public synchronized <T extends CallStackObjectBase> T pop(Class<T> expectedType) {
        Class<? extends CallStackObjectBase> actualType = peek().getClass();
        if (!expectedType.isAssignableFrom(actualType) && !actualType.equals(Expression.class)) {
            throw new ParseCancellationException(
                    "Type mismatch. Expected " + expectedType.getSimpleName() + " instead of "
                            + this.peek().getClass().getSimpleName());
        }
        return expectedType.cast(this.pop());
    }
}
