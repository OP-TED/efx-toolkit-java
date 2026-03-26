<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-2" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*">
        <let name="ctx" value="."/>
        <assert id="R-CTX-001" role="ERROR" diagnostics="ND-Root_BT-00-Text" test="not($ctx/PathNode/TextField/normalize-space(text()) = '')">rule|text|R-CTX-001</assert>
    </rule>
</pattern>
