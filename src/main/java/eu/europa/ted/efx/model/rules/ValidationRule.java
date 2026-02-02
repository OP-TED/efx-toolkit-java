/*
 * Copyright 2022 European Union
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
package eu.europa.ted.efx.model.rules;

import eu.europa.ted.efx.model.Context;
import eu.europa.ted.efx.model.ParsedEntity;
import eu.europa.ted.efx.model.expressions.scalar.BooleanExpression;

public class ValidationRule implements ParsedEntity {

    String id;
    Context subject;
    RuleSeverity severity;
    RuleNature nature = RuleNature.STATIC;
    BooleanExpression condition;
    BooleanExpression invertedCondition;
    BooleanExpression expression;
    BooleanExpression invertedConditionOrExpression;
    NoticeSubtypeRange noticeSubtypes;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }

    public void setSubject(Context subject) {
        this.subject = subject;
    }
    public Context getSubject() {
        return this.subject;
    }

    public void setSeverity(RuleSeverity severity) {
        this.severity = severity;
    }
    public RuleSeverity getSeverity() {
        return this.severity;
    }

    public void setNature(RuleNature nature) {
        this.nature = nature;
    }
    public RuleNature getNature() {
        return this.nature;
    }

    public void setCondition(BooleanExpression condition, BooleanExpression invertedCondition, BooleanExpression invertedConditionOrExpression) {
        this.condition = condition;
        this.invertedCondition = invertedCondition;
        this.invertedConditionOrExpression = invertedConditionOrExpression;
    }

    public BooleanExpression getCondition() {
        return this.condition;
    }

    public BooleanExpression getInvertedCondition() {
        return this.invertedCondition;
    }

    public void setExpression(BooleanExpression expression, BooleanExpression invertedConditionOrExpression) {
        this.expression = expression;
        this.invertedConditionOrExpression = invertedConditionOrExpression;
    }

    public BooleanExpression getExpression() {
        return this.expression;
    }

    public BooleanExpression getInvertedConditionOrExpressionCombination() {
        return this.invertedConditionOrExpression;
    }

    public void setNoticeSubtypes(NoticeSubtypeRange noticeSubtypes) {
        this.noticeSubtypes = noticeSubtypes;
    }
    public NoticeSubtypeRange getNoticeSubtypes() {
        return this.noticeSubtypes;
    }
}
