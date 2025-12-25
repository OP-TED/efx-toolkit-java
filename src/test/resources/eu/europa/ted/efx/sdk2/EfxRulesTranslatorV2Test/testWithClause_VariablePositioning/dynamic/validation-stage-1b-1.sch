<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1b-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <let name="stage1bVar" value="&quot;1B&quot;"/>
    <rule context="/*/PathNode/TextField">
        <assert id="R-E3F-L2G" role="error" test="$stage1bVar != ''">rule|text|R-E3F-L2G</assert>
    </rule>
</pattern>
