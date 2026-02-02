<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <let name="stageVar" value="&quot;first&quot;"/>
    <rule context="/*/PathNode/TextField">
        <assert id="R-K7P-M2Q" role="ERROR" test="$stageVar != ''">rule|text|R-K7P-M2Q</assert>
    </rule>
</pattern>
