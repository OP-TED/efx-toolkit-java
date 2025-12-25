<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-2" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/SubNode">
        <assert id="R-K7P-M2Q" role="error" diagnostics="ND-SubNode_BT-00-Text" test="(not(../PathNode/TextField)) or (../PathNode/TextField/normalize-space(text()) = 'open')">rule|text|R-K7P-M2Q</assert>
    </rule>
</pattern>
