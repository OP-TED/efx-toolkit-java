<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-2" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/SubNode/RepeatableInSubNode/Number">
        <let name="ctx" value="."/>
        <assert id="R-CTX-001" role="error" test="$ctx + 1 &gt; 0">rule|text|R-CTX-001</assert>
    </rule>
</pattern>
