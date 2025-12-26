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
package eu.europa.ted.efx.interfaces;

import java.io.IOException;
import java.util.Map;

import eu.europa.ted.efx.model.rules.CompleteValidation;

/**
 * Interface for generating validation output from the intermediate validation model.
 *
 * This interface abstracts the generation of validation files from the intermediate
 * representation (ValidationStage) produced by the EFX Rules translator. Different
 * implementations can produce different output formats (e.g., Schematron, JavaScript).
 *
 * The typical workflow is:
 * 1. EFX Rules are parsed and translated into an intermediate model (List of ValidationStage)
 * 2. The ValidatorGenerator transforms this intermediate model into the target format
 */
public interface ValidatorGenerator {

    /**
     * Generates validation output files from the intermediate model.
     *
     * @param completeValidation The complete validation model containing stages,
     *                           global variables, and notice subtypes.
     * @return A map of filename to file content for all generated validation files.
     * @throws IOException If an error occurs during file generation.
     */
    Map<String, String> generateOutput(
        CompleteValidation completeValidation
    ) throws IOException;
}
