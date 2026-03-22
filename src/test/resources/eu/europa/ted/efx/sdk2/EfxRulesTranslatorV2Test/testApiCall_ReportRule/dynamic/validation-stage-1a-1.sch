<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/PathNode/TextField">
        <assert id="R-S1A-001" role="ERROR" test=".">rule|text|R-S1A-001</assert>
        <let name="__apiResult1" value="efx:call-api('default', 'check', (../IdField/normalize-space(text())))"/>
        <assert id="R-S1A-002-api-error-1" role="ERROR" test="not($__apiResult1 = -1)">rule|text|api-error</assert>
        <report id="R-S1A-002" role="WARNING" test="not($__apiResult1 = -1) and (not($__apiResult1 = 1))">rule|text|R-S1A-002</report>
    </rule>
    <rule context="/*/PathNode/TextField">
        <report id="R-S1A-003" role="WARNING" test="./normalize-space(text()) = ''">rule|text|R-S1A-003</report>
        <let name="__apiResult2" value="efx:call-api('default', 'check', (../IdField/normalize-space(text())))"/>
        <assert id="R-S1A-004-api-error-1" role="ERROR" test="not($__apiResult2 = -1)">rule|text|api-error</assert>
        <assert id="R-S1A-004" role="ERROR" test="($__apiResult2 = -1) or ($__apiResult2 = 1) or (not(../IndicatorField))">rule|text|R-S1A-004</assert>
    </rule>
</pattern>
