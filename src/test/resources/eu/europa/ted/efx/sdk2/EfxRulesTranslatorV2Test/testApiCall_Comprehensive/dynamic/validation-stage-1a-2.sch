<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-2" xmlns="http://purl.oclc.org/dsdl/schematron">
    <let name="stageStatus" value="efx:call-api('default', 'confirm-status', (/*/PathNode/CodeField/normalize-space(text())))"/>
    <let name="buyerOk" value="efx:call-api('default', 'check-buyer', (/*/PathNode/IdField/normalize-space(text())))"/>
    <rule context="/*/PathNode/TextField">
        <report id="R-S1A-002" role="WARNING" test="./normalize-space(text()) = ''">rule|text|R-S1A-002</report>
        <let name="__apiResult2" value="efx:call-api('default', 'check-buyer', (../IdField/normalize-space(text())))"/>
        <assert id="R-S1A-004-api-error-1" role="ERROR" test="not($__apiResult2 = -1)">rule|text|api-error</assert>
        <let name="__apiResult3" value="efx:call-api('ext-db', 'verify-code', (../IdField/normalize-space(text()), ../CodeField/normalize-space(text())))"/>
        <assert id="R-S1A-004-api-error-2" role="WARNING" test="not($__apiResult3 = -1)">rule|text|api-warning</assert>
        <assert id="R-S1A-004" role="ERROR" test="($__apiResult2 = -1) or ($__apiResult3 = -1) or ($__apiResult2 = 1 and $__apiResult3 = 1)">rule|text|R-S1A-004</assert>
        <let name="__apiResult4" value="efx:call-api('default', 'confirm-status', (../IdField/normalize-space(text())))"/>
        <assert id="R-S1A-005-api-error-1" role="WARNING" test="not($__apiResult4 = -1)">auxiliary|text|status-unavailable</assert>
        <report id="R-S1A-005" role="WARNING" test="not($__apiResult4 = -1) and (not($__apiResult4 = 1))">rule|text|R-S1A-005</report>
    </rule>
    <rule context="/*/PathNode/TextField">
        <assert id="R-S1A-008-api-error-globalCheck" role="ERROR" test="not($globalCheck = -1)">rule|text|api-error</assert>
        <assert id="R-S1A-008-api-error-stageStatus" role="WARNING" test="not($stageStatus = -1)">auxiliary|text|status-unavailable</assert>
        <report id="R-S1A-008" role="WARNING" test="not($globalCheck = -1) and not($stageStatus = -1) and (not($stageStatus = 1) and not($globalCheck = 1))">rule|text|R-S1A-008</report>
        <assert id="R-S1A-009-api-error-stageStatus" role="WARNING" test="not($stageStatus = -1)">auxiliary|text|status-unavailable</assert>
        <let name="__apiResult6" value="efx:call-api('default', 'check-buyer', (../IdField/normalize-space(text())))"/>
        <assert id="R-S1A-009-api-error-1" role="ERROR" test="not($__apiResult6 = -1)">rule|text|api-error</assert>
        <assert id="R-S1A-009" role="ERROR" test="($stageStatus = -1) or ($__apiResult6 = -1) or ($stageStatus = 1 and $__apiResult6 = 1) or (not(../IndicatorField))">rule|text|R-S1A-009</assert>
        <assert id="R-S1A-011-api-error-stageStatus" role="WARNING" test="not($stageStatus = -1)">auxiliary|text|status-unavailable</assert>
        <assert id="R-S1A-011" role="ERROR" test="($stageStatus = -1) or (.) or (not($stageStatus = 1))">rule|text|R-S1A-011</assert>
    </rule>
</pattern>
