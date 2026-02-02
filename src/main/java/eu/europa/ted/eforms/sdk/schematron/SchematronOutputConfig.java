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
package eu.europa.ted.eforms.sdk.schematron;

import java.util.EnumSet;

import eu.europa.ted.efx.model.rules.RuleNature;

/**
 * Configuration for Schematron output generation.
 * Pairs a folder name with the set of rule natures to include in that output.
 */
public final class SchematronOutputConfig {

    /**
     * Static output configuration - includes only static rules.
     * Use when external services are not available.
     */
    public static final SchematronOutputConfig STATIC =
        new SchematronOutputConfig("static", EnumSet.of(RuleNature.STATIC));

    /**
     * Dynamic output configuration - includes all rules (static and dynamic).
     * Use for full validation when external services are available.
     */
    public static final SchematronOutputConfig DYNAMIC =
        new SchematronOutputConfig("dynamic", EnumSet.allOf(RuleNature.class));

    private final String folderName;
    private final EnumSet<RuleNature> ruleNatures;

    public SchematronOutputConfig(String folderName, EnumSet<RuleNature> ruleNatures) {
        this.folderName = folderName;
        this.ruleNatures = ruleNatures;
    }

    public String folderName() {
        return this.folderName;
    }

    public EnumSet<RuleNature> ruleNatures() {
        return this.ruleNatures;
    }
}
