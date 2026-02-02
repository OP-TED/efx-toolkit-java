<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-3-3" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/PathNode/TextField">
        <assert id="R-F5V-T6B" role="ERROR" test="../IndicatorField">rule|text|R-F5V-T6B</assert>
        <report id="R-W1D-J2Y" role="INFO" test="../IndicatorField = true()">rule|text|R-W1D-J2Y</report>
    </rule>
</pattern>
