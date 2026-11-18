<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-4a-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/PathNode/TextField/@Attribute">
        <assert id="R-EVR-008" role="ERROR" test="exists(.)">rule|text|R-EVR-008</assert>
    </rule>
</pattern>
