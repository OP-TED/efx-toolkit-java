<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-2a-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/PathNode/TextField">
        <assert id="R-S2A-001" role="ERROR" test=".">rule|text|R-S2A-001</assert>
    </rule>
    <rule context="/*/SubNode">
        <report id="R-S2A-004" role="WARNING" diagnostics="ND-SubNode_BT-01-SubLevel-Text" test="../PathNode/ChildNode/SubLevelTextField/normalize-space(text()) = ''">rule|text|R-S2A-004</report>
    </rule>
</pattern>
