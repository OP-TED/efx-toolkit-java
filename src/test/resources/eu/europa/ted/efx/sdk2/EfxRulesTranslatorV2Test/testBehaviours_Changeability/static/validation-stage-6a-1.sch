<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-6a-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*">
        <assert id="R-CHG-005" role="ERROR" diagnostics="ND-Root_BT-00-Text" test="PathNode/TextField/normalize-space(text()) = 'fixed'">rule|text|R-CHG-005</assert>
    </rule>
</pattern>
