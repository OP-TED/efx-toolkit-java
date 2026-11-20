<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-2" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/PathNode/TextField">
        <assert id="R-VAR-004" role="ERROR" test="$cutoff &lt; xs:date('2025-01-01Z')">rule|text|R-VAR-004</assert>
    </rule>
</pattern>
