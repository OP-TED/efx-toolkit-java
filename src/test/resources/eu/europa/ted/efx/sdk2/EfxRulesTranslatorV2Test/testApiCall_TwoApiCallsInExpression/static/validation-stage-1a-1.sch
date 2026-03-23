<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/PathNode/TextField">
        <assert id="R-S1A-001" role="ERROR" test=".">rule|text|R-S1A-001</assert>
    </rule>
    <rule context="/*/PathNode/TextField">
        <report id="R-S1A-003" role="WARNING" test="./normalize-space(text()) = ''">rule|text|R-S1A-003</report>
    </rule>
</pattern>
