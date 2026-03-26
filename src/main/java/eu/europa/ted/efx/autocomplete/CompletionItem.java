/*
 * Copyright 2026 European Union
 *
 * Licensed under the EUPL, Version 1.2 or - as soon they will be approved by the European
 * Commission - subsequent versions of the EUPL (the "Licence"); You may not use this work except in
 * compliance with the Licence. You may obtain a copy of the Licence at:
 * https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence
 * is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the Licence for the specific language governing permissions and limitations under
 * the Licence.
 */
package eu.europa.ted.efx.autocomplete;

import java.util.List;

/**
 * An EFX autocomplete suggestion for code editors.
 */
public class CompletionItem {
    private final String label;
    private final CompletionKind kind;
    private final EfxDataType dataType;
    private final List<EfxFunctionParameter> parameters;

    public CompletionItem(String label, CompletionKind kind, EfxDataType dataType,
        List<EfxFunctionParameter> parameters) {
        this.label = label;
        this.kind = kind;
        this.dataType = dataType;
        this.parameters = parameters;
    }

    public String getLabel() { return label; }
    public CompletionKind getKind() { return kind; }
    public EfxDataType getDataType() { return dataType; }
    public List<EfxFunctionParameter> getParameters() { return parameters; }
}
