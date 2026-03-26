<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-2a-2" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/PathNode/TextField">
        <assert id="R-S2A-001" role="ERROR" test=".">rule|text|R-S2A-001</assert>
        <let name="__apiResult10" value="efx:call-api('ext-db', 'verify-code', (../IdField/normalize-space(text()), ../CodeField/normalize-space(text())))"/>
        <assert id="R-S2A-003-api-error-1" role="WARNING" test="not($__apiResult10 = -1)">rule|text|api-warning</assert>
        <let name="__apiResult11" value="efx:call-api('default', 'confirm-status', (../IdField/normalize-space(text())))"/>
        <assert id="R-S2A-003-api-error-2" role="WARNING" test="not($__apiResult11 = -1)">auxiliary|text|status-unavailable</assert>
        <report id="R-S2A-003" role="INFO" test="not($__apiResult10 = -1) and not($__apiResult11 = -1) and (not($__apiResult10 = 1) and not($__apiResult11 = 1))">rule|text|R-S2A-003</report>
    </rule>
    <rule context="/*/SubNode">
        <assert id="R-S2A-005-api-error-globalCheck" role="ERROR" test="not($globalCheck = -1)">rule|text|api-error</assert>
        <let name="__apiResult12" value="efx:call-api('staging', 'validate-ref', (../PathNode/IdField/normalize-space(text())))"/>
        <assert id="R-S2A-005-api-error-1" role="ERROR" test="not($__apiResult12 = -1)">auxiliary|text|ref-check-failed</assert>
        <assert id="R-S2A-005" role="ERROR" diagnostics="ND-SubNode_BT-01-SubLevel-Text" test="($globalCheck = -1) or ($__apiResult12 = -1) or ($__apiResult12 = 1 and $globalCheck = 1) or (not(../PathNode/IndicatorField))">rule|text|R-S2A-005</assert>
    </rule>
</pattern>
