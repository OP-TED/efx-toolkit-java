<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <let name="stageOk" value="efx:call-api($apiUrl-default, 'confirm-status', (/*/PathNode/CodeField/normalize-space(text())))"/>
    <let name="statusOk" value="efx:call-api($apiUrl-default, 'confirm-status', (/*/PathNode/CodeField/normalize-space(text())))"/>
    <rule context="/*/PathNode/TextField">
        <let name="buyerOk" value="efx:call-api($apiUrl-default, 'check-buyer', (../IdField/normalize-space(text())))"/>
        <assert id="R-S1A-001" role="ERROR" test=".">rule|text|R-S1A-001</assert>
        <assert id="R-S1A-002-api-error-stageOk" role="WARNING" test="not($stageOk = -1)">rule|text|api-warning</assert>
        <assert id="R-S1A-002-api-error-buyerOk" role="ERROR" test="not($buyerOk = -1)">rule|text|api-error</assert>
        <let name="__apiResult1" value="efx:call-api($apiUrl-default, 'verify-code', (../IdField/normalize-space(text()), ../CodeField/normalize-space(text())))"/>
        <assert id="R-S1A-002-api-error-1" role="ERROR" test="not($__apiResult1 = -1)">rule|text|api-error</assert>
        <assert id="R-S1A-002" role="ERROR" test="($stageOk = -1) or ($buyerOk = -1) or ($__apiResult1 = -1) or ($buyerOk = 1 and $stageOk = 1 and $__apiResult1 = 1)">rule|text|R-S1A-002</assert>
    </rule>
    <rule context="/*/PathNode/TextField">
        <report id="R-S1A-003" role="WARNING" test="./normalize-space(text()) = ''">rule|text|R-S1A-003</report>
        <assert id="R-S1A-004-api-error-globalOk" role="ERROR" test="not($globalOk = -1)">rule|text|api-error</assert>
        <assert id="R-S1A-004-api-error-stageOk" role="WARNING" test="not($stageOk = -1)">rule|text|api-warning</assert>
        <assert id="R-S1A-004" role="ERROR" test="($globalOk = -1) or ($stageOk = -1) or ($stageOk = 1 and $globalOk = 1) or (not(../IndicatorField))">rule|text|R-S1A-004</assert>
    </rule>
</pattern>
