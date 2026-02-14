<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-4" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/PathNode/TextField">
        <assert id="R-K7P-M2Q" role="ERROR" test=".">rule|text|R-K7P-M2Q</assert>
        <assert id="R-X3F-N8W" role="WARNING" test="not(./normalize-space(text()) = '')">rule|text|R-X3F-N8W</assert>
    </rule>
</pattern>
