package eu.europa.ted.efx.model.constructs;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import eu.europa.ted.efx.model.expressions.TypedExpression;
import eu.europa.ted.efx.model.expressions.scalar.BooleanExpression;
import eu.europa.ted.efx.model.expressions.scalar.NumericExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringExpression;
import eu.europa.ted.efx.model.expressions.sequence.SequenceExpression;

/**
 * The arguments of a call, in order, each with the parameter it is given for.
 *
 * <p>One list serves every construct: a built-in, an expression construct or a clause as much as
 * a function or a template declared in EFX. The arguments of a resolved call are made by
 * {@link Parameters#match}: each is converted to its parameter, with {@code T} bound. The
 * overload of a {@link Composable} reads them as the expression classes its signature gives them,
 * and what a clause belongs to reads them with the typed accessors. A template block passes its
 * arguments down to the blocks it holds.</p>
 *
 * <p>Two argument lists are equal as lists are: argument by argument, each by its parameter and its
 * value.</p>
 *
 * @param <P> the kind of parameter: declared by a signature, or parsed with a function or a
 *        template declared in EFX
 */
public class Arguments<P extends Parameter> extends LinkedList<Argument<P>> {

    /**
     * Pairs each parameter with the value at its position.
     *
     * @param parameters the parameter of each position, in order
     * @param values the value of each position, one for each parameter
     */
    public Arguments(List<? extends P> parameters, List<? extends TypedExpression> values) {
        super(IntStream.range(0, parameters.size())
                .mapToObj(position -> new Argument<P>(parameters.get(position),
                        values.get(position)))
                .collect(Collectors.toList()));
    }

    /**
     * @return the values of the arguments, in order
     */
    public List<TypedExpression> getArgumentValues() {
        return this.stream().map(argument -> argument.value).collect(Collectors.toList());
    }

    /**
     * Gives the argument at a position as an expression of its parameter's class.
     *
     * @param <T> the expression class of the parameter at that position, or a supertype of it
     * @param position the position of the argument
     * @param type the expression class of the parameter at that position, or a supertype of it
     * @return the argument
     */
    public <T extends TypedExpression> T at(final int position, final Class<T> type) {
        return type.cast(this.get(position).value);
    }

    /**
     * Gives the expression class an argument was converted to, which for {@code T} or {@code T*} is
     * the class of the value primitive {@code T} is bound to.
     *
     * @param position the position of the argument
     * @return the expression class of the parameter at that position
     */
    public Class<? extends TypedExpression> typeAt(final int position) {
        return this.get(position).parameter.getType().getExpressionType();
    }

    /**
     * Gives the sequence class an argument was converted to, for a parameter that is a sequence.
     *
     * @param position the position of the argument
     * @return the sequence class of the parameter at that position
     */
    public Class<? extends SequenceExpression> sequenceTypeAt(final int position) {
        return this.typeAt(position).asSubclass(SequenceExpression.class);
    }

    /**
     * @param position the position of a text argument
     * @return the argument
     */
    public StringExpression stringAt(final int position) {
        return this.at(position, StringExpression.class);
    }

    /**
     * @param position the position of a number argument
     * @return the argument
     */
    public NumericExpression numericAt(final int position) {
        return this.at(position, NumericExpression.class);
    }

    /**
     * @param position the position of an indicator argument
     * @return the argument
     */
    public BooleanExpression booleanAt(final int position) {
        return this.at(position, BooleanExpression.class);
    }

    /**
     * @param position the position of a sequence argument
     * @return the argument
     */
    public SequenceExpression sequenceAt(final int position) {
        return this.at(position, SequenceExpression.class);
    }

    /**
     * Gives the arguments of a variadic tail as expressions of its parameter's class.
     *
     * @param <T> the expression class of the tail, or a supertype of it
     * @param position the position of the first argument of the tail
     * @param type the expression class of the tail, or a supertype of it
     * @return the arguments from that position to the last
     */
    public <T extends TypedExpression> List<T> from(final int position, final Class<T> type) {
        return this.subList(position, this.size()).stream()
                .map(argument -> type.cast(argument.value))
                .collect(Collectors.toList());
    }
}
