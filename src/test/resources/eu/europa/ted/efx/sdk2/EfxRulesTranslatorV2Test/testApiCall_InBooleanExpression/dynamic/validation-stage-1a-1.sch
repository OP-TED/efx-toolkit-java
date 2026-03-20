<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/PathNode/TextField">
        <assert id="R-S1A-001" role="ERROR" test=".">rule|text|R-S1A-001</assert>
    </rule>
    <rule context="/*/PathNode/TextField">
        <let name="__apiResult1" value="efx:call-api($apiUrl-default, 'check', (../IdField/normalize-space(text())))"/>
        <assert id="R-K7P-M2Q-api-error-1" role="ERROR" test="not($__apiResult1 = -1)">rule|text|api-error</assert>
        <assert id="R-K7P-M2Q" role="ERROR" test="($__apiResult1 = -1) or (. and not($__apiResult1 = 1))">rule|text|R-K7P-M2Q</assert>
    </rule>
</pattern>
