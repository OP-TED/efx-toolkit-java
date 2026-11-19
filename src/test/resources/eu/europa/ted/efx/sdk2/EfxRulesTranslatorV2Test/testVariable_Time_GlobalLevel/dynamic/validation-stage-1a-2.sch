<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-2" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/PathNode/TextField">
        <assert id="R-VAR-005" role="ERROR" test="$opening &lt; xs:time('17:00:00Z')">rule|text|R-VAR-005</assert>
    </rule>
</pattern>
