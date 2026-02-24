<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/PathNode/TextField">
        <report id="R-K7P-M2Q" role="WARNING" test="./normalize-space(text()) = ''">rule|text|R-K7P-M2Q</report>
    </rule>
</pattern>
