<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/PathNode/TextField">
        <let name="__apiResult1" value="number(if (exists(../IndicatorField)) then number(efx:call-api('default', 'check', string-join((concat('arg1=', encode-for-uri(((../IdField/normalize-space(text())), '')[1]))), '&amp;'))) else 0)"/>
        <let name="__apiResult2" value="number(efx:call-api('default', 'check', string-join((concat('arg1=', encode-for-uri(((../CodeField/normalize-space(text())), '')[1]))), '&amp;')))"/>
        <let name="__apiResult3" value="number(efx:call-api('default', 'check', string-join((concat('arg1=', encode-for-uri(((../CodeField/normalize-space(text())), '')[1]))), '&amp;')))"/>
        <let name="__apiResult4" value="number(efx:call-api('default', 'confirm', string-join((concat('arg1=', encode-for-uri(((for $__value in ($__apiResult3 = 1) return string(boolean($__value))), '')[1]))), '&amp;')))"/>
        <let name="__apiResult5" value="number(if ($__apiResult4 = 1) then number(efx:call-api('default', 'check', string-join((concat('arg1=', encode-for-uri(((../IdField/normalize-space(text())), '')[1]))), '&amp;'))) else 0)"/>
        <let name="__apiResult6" value="number(if (exists(../IndicatorField)) then number(efx:call-api('default', 'check', string-join((concat('arg1=', encode-for-uri(((../IdField/normalize-space(text())), '')[1]))), '&amp;'))) else 0)"/>
        <assert id="R-S1A-001-api-error-1" role="ERROR" test="not($__apiResult1 = -1)">auxiliary|text|api-error</assert>
        <assert id="R-S1A-001" role="ERROR" test="($__apiResult1 = -1) or ($__apiResult1 = 1) or (not(exists(../IndicatorField)))">rule|text|R-S1A-001</assert>
        <assert id="R-S1A-002-api-error-1" role="ERROR" test="not($__apiResult2 = -1)">auxiliary|text|api-error</assert>
        <assert id="R-S1A-002" role="ERROR" test="($__apiResult2 = -1) or (exists(.)) or (not($__apiResult2 = 1))">rule|text|R-S1A-002</assert>
        <assert id="R-S1A-003-api-error-1" role="ERROR" test="not($__apiResult3 = -1)">auxiliary|text|api-error</assert>
        <assert id="R-S1A-003-api-error-2" role="ERROR" test="not($__apiResult4 = -1)">auxiliary|text|api-error</assert>
        <assert id="R-S1A-003-api-error-3" role="ERROR" test="not($__apiResult5 = -1)">auxiliary|text|api-error</assert>
        <assert id="R-S1A-003" role="ERROR" test="($__apiResult3 = -1) or ($__apiResult4 = -1) or ($__apiResult5 = -1) or ($__apiResult5 = 1) or (not($__apiResult4 = 1))">rule|text|R-S1A-003</assert>
        <assert id="R-S1A-004-api-error-1" role="ERROR" test="not($__apiResult6 = -1)">auxiliary|text|api-error</assert>
        <report id="R-S1A-004" role="WARNING" test="not($__apiResult6 = -1) and ((exists(../IndicatorField)) and ($__apiResult6 = 1))">rule|text|R-S1A-004</report>
    </rule>
</pattern>
