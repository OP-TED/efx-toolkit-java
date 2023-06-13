package eu.europa.ted.efx.sdk1.xpath;

import eu.europa.ted.eforms.sdk.component.SdkComponent;
import eu.europa.ted.eforms.sdk.component.SdkComponentType;
import eu.europa.ted.efx.interfaces.TranslatorOptions;
import eu.europa.ted.efx.model.Expression;
import eu.europa.ted.efx.model.Expression.MultilingualStringExpression;
import eu.europa.ted.efx.model.Expression.MultilingualStringListExpression;
import eu.europa.ted.efx.model.Expression.PathExpression;
import eu.europa.ted.efx.xpath.XPathContextualizer;
import eu.europa.ted.efx.xpath.XPathScriptGenerator;

@SdkComponent(versions = {"0.6", "0.7", "1"}, componentType = SdkComponentType.SCRIPT_GENERATOR)
public class XPathScriptGeneratorV1 extends XPathScriptGenerator {

    public XPathScriptGeneratorV1(TranslatorOptions translatorOptions) {
        super(translatorOptions);
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
     * Both EFX and EFX 2 implementations of the feature rely on the existence of a
     * "ted:preferred-languages()" function in the XSLT.
     * This function returns the list of languages used in the visualisation in the
     * order of preference (visualisation language followed by notice language(s)).
     */
    @Override
    public <T extends Expression> T composeFieldValueReference(PathExpression fieldReference, Class<T> type) {
        if ((MultilingualStringExpression.class.isAssignableFrom(type) || MultilingualStringListExpression.class.isAssignableFrom(type)) && !XPathContextualizer.hasPredicate(fieldReference, "@languageID")) {
            PathExpression languageSpecific = XPathContextualizer.addPredicate(fieldReference, "@languageID=$__LANG__");
            String script = "(for $__LANG__ in ted:preferred-languages() return " + languageSpecific.script + "/normalize-space(text()), " + fieldReference.script + "/normalize-space(text()))[1]";
            return Expression.instantiate(script, type);
          }
        return super.composeFieldValueReference(fieldReference, type);
    }
}
