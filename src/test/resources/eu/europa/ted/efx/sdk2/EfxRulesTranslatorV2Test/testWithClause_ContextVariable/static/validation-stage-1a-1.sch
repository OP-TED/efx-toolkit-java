<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/PathNode/TextField">
        <let name="ctx" value="."/>
        <assert id="R-K7P-M2Q" role="ERROR" test="not($ctx = '')">rule|text|R-K7P-M2Q</assert>
    </rule>
</pattern>
