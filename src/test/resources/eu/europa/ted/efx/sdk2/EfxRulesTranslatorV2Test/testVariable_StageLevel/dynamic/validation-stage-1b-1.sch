<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1b-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <let name="stageVar" value="&quot;second&quot;"/>
    <rule context="/*/PathNode/TextField">
        <assert id="R-X3F-N8W" role="ERROR" test="$stageVar != ''">rule|text|R-X3F-N8W</assert>
    </rule>
</pattern>
