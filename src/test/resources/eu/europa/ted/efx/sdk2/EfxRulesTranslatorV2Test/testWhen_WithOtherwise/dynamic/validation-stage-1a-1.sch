<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/PathNode/TextField">
        <assert id="R-K7P-M2Q" role="ERROR" test="(exists(../NumberField)) or (not(./normalize-space(text()) = 'open'))">rule|text|R-K7P-M2Q</assert>
        <report id="R-X3F-N8W" role="INFO" test="(./normalize-space(text()) = 'restricted') and (../NumberField/number() &gt; 0)">rule|text|R-X3F-N8W</report>
    </rule>
</pattern>
