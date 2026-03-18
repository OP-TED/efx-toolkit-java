<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-2a-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/PathNode/TextField">
        <assert id="R-S2A-001" role="ERROR" test=".">rule|text|R-S2A-001</assert>
        <let name="__apiResult3" value="efx:call-api('default', 'check', ../IdField/normalize-space(text()))"/>
        <assert id="R-S2A-002-api-error-1" role="ERROR" test="not($__apiResult3 = -1)">rule|text|api-error</assert>
        <assert id="R-S2A-002" role="ERROR" test="($__apiResult3 = -1) or ($__apiResult3 = 1)">rule|text|R-S2A-002</assert>
    </rule>
    <rule context="/*/SubNode">
        <report id="R-S2A-003" role="WARNING" diagnostics="ND-SubNode_BT-01-SubLevel-Text" test="../PathNode/ChildNode/SubLevelTextField/normalize-space(text()) = ''">rule|text|R-S2A-003</report>
        <let name="__apiResult4" value="efx:call-api('default', 'check', ../PathNode/IdField/normalize-space(text()))"/>
        <assert id="R-S2A-004-api-error-1" role="ERROR" test="not($__apiResult4 = -1)">rule|text|api-error</assert>
        <assert id="R-S2A-004" role="ERROR" diagnostics="ND-SubNode_BT-01-SubLevel-Text" test="($__apiResult4 = -1) or ($__apiResult4 = 1)">rule|text|R-S2A-004</assert>
    </rule>
</pattern>
