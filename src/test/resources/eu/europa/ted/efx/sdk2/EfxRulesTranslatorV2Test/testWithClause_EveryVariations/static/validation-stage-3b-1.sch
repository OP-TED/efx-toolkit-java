<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-3b-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/SubNode/SubSubNode/SubTextField">
        <let name="text" value="."/>
        <assert id="R-EVR-007" role="ERROR" test="exists(.)">rule|text|R-EVR-007</assert>
    </rule>
</pattern>
