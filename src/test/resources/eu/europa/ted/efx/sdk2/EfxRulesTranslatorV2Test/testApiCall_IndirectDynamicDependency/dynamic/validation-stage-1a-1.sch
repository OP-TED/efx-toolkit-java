<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <let name="apiResult" value="efx:call-api('default', 'check-buyer', /*/PathNode/IdField/normalize-space(text()))"/>
    <let name="alias" value="$apiResult = 1 and /*/PathNode/TextField"/>
    <let name="alias2" value="$alias or /*/PathNode/IndicatorField"/>
    <rule context="/*/PathNode/TextField">
        <assert id="R-S1A-001-api-error-apiResult" role="ERROR" test="not($apiResult = -1)">rule|text|api-error</assert>
        <assert id="R-S1A-001" role="ERROR" test="($apiResult = -1) or ($alias)">rule|text|R-S1A-001</assert>
        <assert id="R-S1A-002-api-error-apiResult" role="ERROR" test="not($apiResult = -1)">rule|text|api-error</assert>
        <assert id="R-S1A-002" role="ERROR" test="($apiResult = -1) or ($alias2)">rule|text|R-S1A-002</assert>
        <assert id="R-S1A-003" role="ERROR" test=".">rule|text|R-S1A-003</assert>
    </rule>
</pattern>