<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/PathNode/TextField">
        <report id="R-X3F-N8W" role="WARNING" test="./normalize-space(text()) = ''">rule|text|R-X3F-N8W</report>
        <assert id="R-A1B-C2D" role="INFO" test="not(./normalize-space(text()) = '')">rule|text|R-A1B-C2D</assert>
    </rule>
</pattern>
