<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/PathNode/TextField">
        <assert id="R-VAR-002" role="ERROR" test="$threshold &gt; 0">rule|text|R-VAR-002</assert>
    </rule>
</pattern>
