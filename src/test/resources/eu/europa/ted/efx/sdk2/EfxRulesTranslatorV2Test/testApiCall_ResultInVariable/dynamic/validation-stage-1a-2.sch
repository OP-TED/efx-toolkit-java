<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-2" xmlns="http://purl.oclc.org/dsdl/schematron">
    <let name="stageOk" value="number(efx:call-api('default', 'confirm-status', string-join((concat('arg1=', encode-for-uri(((/*/PathNode/CodeField/normalize-space(text())), '')[1]))), '&amp;')))"/>
    <rule context="/*/PathNode/TextField">
        <report id="R-S1A-003" role="WARNING" test="./normalize-space(text()) = ''">rule|text|R-S1A-003</report>
        <assert id="R-S1A-004-api-error-globalOk" role="ERROR" test="not($globalOk = -1)">auxiliary|text|api-error</assert>
        <assert id="R-S1A-004-api-error-stageOk" role="WARNING" test="not($stageOk = -1)">auxiliary|text|api-warning</assert>
        <assert id="R-S1A-004" role="ERROR" test="($globalOk = -1) or ($stageOk = -1) or ($stageOk = 1 and $globalOk = 1) or (not(exists(../IndicatorField)))">rule|text|R-S1A-004</assert>
    </rule>
</pattern>
