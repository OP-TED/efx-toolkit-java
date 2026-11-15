<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/PathNode/TextField">
        <let name="__apiResult1" value="number(efx:call-api('default', 'check-default', (../IdField/normalize-space(text()))))"/>
        <let name="__apiResult2" value="number(efx:call-api('default', 'check-warn', (../IdField/normalize-space(text()))))"/>
        <let name="__apiResult3" value="number(efx:call-api('default', 'check-reject', (../IdField/normalize-space(text()))))"/>
        <assert id="R-S1A-001" role="ERROR" test=".">rule|text|R-S1A-001</assert>
        <assert id="R-S1A-002-api-error-1" role="ERROR" test="not($__apiResult1 = -1)">rule|text|api-error</assert>
        <assert id="R-S1A-002-api-error-2" role="WARNING" test="not($__apiResult2 = -1)">auxiliary|text|custom-warning</assert>
        <assert id="R-S1A-002-api-error-3" role="ERROR" test="not($__apiResult3 = -1)">auxiliary|text|custom-error</assert>
        <assert id="R-S1A-002" role="ERROR" test="($__apiResult1 = -1) or ($__apiResult2 = -1) or ($__apiResult3 = -1) or ($__apiResult1 = 1 and $__apiResult2 = 1 and $__apiResult3 = 1)">rule|text|R-S1A-002</assert>
    </rule>
    <rule context="/*/PathNode/TextField">
        <let name="__apiResult4" value="if (../IndicatorField) then number(efx:call-api('default', 'check-warn', (../IdField/normalize-space(text())))) else 0"/>
        <report id="R-S1A-003" role="WARNING" test="./normalize-space(text()) = ''">rule|text|R-S1A-003</report>
        <assert id="R-S1A-004-api-error-1" role="WARNING" test="not($__apiResult4 = -1)">auxiliary|text|custom-warning</assert>
        <assert id="R-S1A-004" role="ERROR" test="($__apiResult4 = -1) or ($__apiResult4 = 1) or (not(../IndicatorField))">rule|text|R-S1A-004</assert>
    </rule>
</pattern>
