<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-2a-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/PathNode/TextField">
        <let name="__apiResult5" value="number(efx:call-api('default', 'check-reject', string-join((concat('arg1=', encode-for-uri(((../IdField/normalize-space(text())), '')[1]))), '&amp;')))"/>
        <assert id="R-S2A-001" role="ERROR" test="exists(.)">rule|text|R-S2A-001</assert>
        <assert id="R-S2A-002-api-error-1" role="ERROR" test="not($__apiResult5 = -1)">auxiliary|text|custom-error</assert>
        <assert id="R-S2A-002" role="ERROR" test="($__apiResult5 = -1) or ($__apiResult5 = 1)">rule|text|R-S2A-002</assert>
    </rule>
    <rule context="/*/SubNode">
        <let name="__apiResult6" value="number(efx:call-api('default', 'check-default', string-join((concat('arg1=', encode-for-uri(((../PathNode/IdField/normalize-space(text())), '')[1]))), '&amp;')))"/>
        <report id="R-S2A-003" role="WARNING" diagnostics="ND-SubNode_BT-01-SubLevel-Text" test="../PathNode/ChildNode/SubLevelTextField/normalize-space(text()) = ''">rule|text|R-S2A-003</report>
        <assert id="R-S2A-004-api-error-1" role="ERROR" test="not($__apiResult6 = -1)">rule|text|api-error</assert>
        <assert id="R-S2A-004" role="ERROR" diagnostics="ND-SubNode_BT-01-SubLevel-Text" test="($__apiResult6 = -1) or ($__apiResult6 = 1)">rule|text|R-S2A-004</assert>
    </rule>
</pattern>
