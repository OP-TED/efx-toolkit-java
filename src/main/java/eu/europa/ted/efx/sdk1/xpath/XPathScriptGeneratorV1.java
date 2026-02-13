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
import eu.europa.ted.efx.model.expressions.scalar.NumericExpression;
import eu.europa.ted.efx.model.expressions.scalar.StringExpression;
import eu.europa.ted.efx.model.types.EfxDataType;
import eu.europa.ted.efx.xpath.XPathScriptGenerator;

@SdkComponent(versions = {"1"}, componentType = SdkComponentType.SCRIPT_GENERATOR)
public class XPathScriptGeneratorV1 extends XPathScriptGenerator {

    public XPathScriptGeneratorV1(TranslatorOptions translatorOptions) {
        super(translatorOptions);
    }

    @Override
    public StringExpression composeToStringConversion(NumericExpression number) {
        String formatString = this.translatorOptions.getDecimalFormat().adaptFormatString("0.##########");
        return new StringExpression("format-number(" + number.getScript() + ", '" + formatString + "')");
    }

    /***
     * This method is overridden to workaround a limitation of EFX 1.
     * 
     * When a multilingual text field is referenced, then a special XPath expression
     * is generated to retrieve the value in the "preferred" language.
     * Preferred language is the first language among the languages listed in the
     * translator options for which a text value is available in the field.
     * 
     * The logic of the workaround is as follows:
     * if the fieldReference is a multilingual text field and it does not
     * already come with a predicate that filters by @languageID, then we add a
     * predicate which, using a for loop, will find the first language for which a
     * value is available in the field.
     * 
     * In EFX 1 therefore the selection of the appropriate (preferred) language is
     * done implicitly, whereas in EFX 2 it is done explicitly by calling a special
     * function designed to perform this task.
     * 
     * Both EFX-1 and EFX-2 implementations of the feature rely on the existence of a
     * $PREFERRED_LANGUAGES variable in the XSLT.
     * This function returns the list of languages used in the visualisation in the
     * order of preference (visualisation language followed by notice language(s)).
     */
    @Override
    public PathExpression composeFieldValueReference(PathExpression fieldReference) {
        XPathInfo xpathInfo = XPathProcessor.parse(fieldReference.getScript());
        if (fieldReference.is(EfxDataType.MultilingualString.class) && !xpathInfo.hasPredicate("@languageID")) {
            return Expression.instantiate("efx:preferred-language-text(" + fieldReference.getScript() + ")", fieldReference.getClass());
        }
        return super.composeFieldValueReference(fieldReference);
    }
}
