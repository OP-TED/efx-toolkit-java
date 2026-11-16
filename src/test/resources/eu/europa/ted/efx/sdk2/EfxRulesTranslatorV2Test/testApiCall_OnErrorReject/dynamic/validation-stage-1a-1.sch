<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/PathNode/TextField">
        <let name="__apiResult1" value="number(efx:call-api('default', 'check', string-join((concat('arg1=', encode-for-uri(((../IdField/normalize-space(text())), '')[1]))), '&amp;')))"/>
        <assert id="R-S1A-001" role="ERROR" test=".">rule|text|R-S1A-001</assert>
        <assert id="R-S1A-002-api-error-1" role="ERROR" test="not($__apiResult1 = -1)">rule|text|api-error</assert>
        <assert id="R-S1A-002" role="ERROR" test="($__apiResult1 = -1) or ($__apiResult1 = 1)">rule|text|R-S1A-002</assert>
    </rule>
    <rule context="/*/PathNode/TextField">
        <let name="__apiResult2" value="number(if (../IndicatorField) then number(efx:call-api('default', 'check', string-join((concat('arg1=', encode-for-uri(((../IdField/normalize-space(text())), '')[1]))), '&amp;'))) else 0)"/>
        <report id="R-S1A-003" role="WARNING" test="./normalize-space(text()) = ''">rule|text|R-S1A-003</report>
        <assert id="R-S1A-004-api-error-1" role="ERROR" test="not($__apiResult2 = -1)">rule|text|api-error</assert>
        <assert id="R-S1A-004" role="ERROR" test="($__apiResult2 = -1) or ($__apiResult2 = 1) or (not(../IndicatorField))">rule|text|R-S1A-004</assert>
    </rule>
</pattern>
