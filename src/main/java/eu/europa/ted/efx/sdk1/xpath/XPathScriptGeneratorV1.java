/*
 * Copyright 2023 European Union
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European
 * Commission – subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package eu.europa.ted.efx.sdk1.xpath;

import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.eforms.xpath.XPathInfo;
import eu.europa.ted.eforms.xpath.XPathProcessor;
import eu.europa.ted.efx.interfaces.TranslatorOptions;
import eu.europa.ted.efx.model.expressions.Expression;
import eu.europa.ted.efx.model.expressions.PathExpression;
import eu.europa.ted.efx.model.expressions.scalar.BooleanExpression;
import eu.europa.ted.efx.model.expressions.scalar.NodePath;
import eu.europa.ted.efx.model.expressions.scalar.NumericExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringLiteral;
import eu.europa.ted.efx.xpath.XPathScriptGenerator;

@SdkComponent(versions = {"1"}, componentType = SdkComponentType.SCRIPT_GENERATOR)
public class XPathScriptGeneratorV1 extends XPathScriptGenerator {

    public XPathScriptGeneratorV1(TranslatorOptions translatorOptions) {
        super(translatorOptions);
    }

    @Override
    public PathExpression composeFieldReferenceWithAxis(final PathExpression fieldReference,
        final String axis) {
        String resultXPath = XPathProcessor.addAxis(axis, fieldReference.getScript());
        return Expression.instantiate(resultXPath, fieldReference.getClass());
    }

    @Override
    public StringExpression composeToStringConversion(NumericExpression number) {
        String formatString = this.translatorOptions.getDecimalFormat().adaptFormatString("0.##########");
        return new StringExpression("format-number(" + number.getScript() + ", '" + formatString + "')");
    }

    /**
     * Preserved V1 behavior: pass EFX string literal through as-is without converting
     * escape sequences to XPath format.
     */
    @Override
    public StringLiteral getStringLiteralEquivalent(String literal) {
        return new StringLiteral(literal);
    }

    /**
     * Preserved V1 behavior: pass EFX pattern literal through as-is without converting
     * escape sequences to XPath format.
     */
    @Override
    public BooleanExpression composePatternMatchCondition(StringExpression expression,
        String pattern) {
        return new BooleanExpression(
            String.format("fn:matches(normalize-space(%s), %s)", expression.getScript(), pattern));
    }

    /***
     * Retrieves the value of a multilingual text field in the "preferred" language.
     * Preferred language is the first language among the languages listed in the
     * translator options for which a text value is available in the field.
     *
     * This is a workaround for a limitation of EFX 1: the language cannot be selected
     * explicitly by the template author, so template translation applies this
     * implicitly to every multilingual field it renders. In EFX 2 the selection is
     * done explicitly, by calling a function designed to perform this task.
     *
     * If the reference already comes with a predicate that filters by @languageID,
     * then the template author has already pinned a language and the value is
     * retrieved as-is.
     */
    @Override
    public StringExpression getTextInPreferredLanguage(final PathExpression fieldReference) {
        final XPathInfo xpathInfo = XPathProcessor.parse(fieldReference.getScript());
        if (xpathInfo.hasPredicate("@languageID")) {
            // The value reference is a PathExpression, which is not a StringExpression and cannot
            // be returned as such. Only the generated script matters here: the caller re-creates
            // the expression using the type of the field reference it started from.
            return Expression.from(super.composeFieldValueReference(fieldReference),
                StringExpression.class);
        }
        return super.getTextInPreferredLanguage(fieldReference);
    }

    @Override
    public PathExpression composeExternalReference(StringExpression externalReference) {
        return new NodePath(
            "fn:doc(concat($urlPrefix, " + externalReference.getScript() + "))");
    }

    @Override
    public PathExpression composeFieldInExternalReference(PathExpression externalReference,
        PathExpression fieldReference) {
        return Expression.instantiate(externalReference.getScript() + fieldReference.getScript(), fieldReference.getClass());
    }
}
