<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/PathNode/TextField">
        <assert id="R-SEQ-004" role="error" test="count($dates) &gt; 0">rule|text|R-SEQ-004</assert>
    </rule>
</pattern>
