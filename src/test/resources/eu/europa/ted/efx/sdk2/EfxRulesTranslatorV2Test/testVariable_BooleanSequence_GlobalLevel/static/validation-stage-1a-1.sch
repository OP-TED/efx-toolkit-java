<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/PathNode/TextField">
        <assert id="R-SEQ-003" role="ERROR" test="count($flags) &gt; 0">rule|text|R-SEQ-003</assert>
    </rule>
</pattern>
