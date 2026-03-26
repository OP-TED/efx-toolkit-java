<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-2a" xmlns="http://purl.oclc.org/dsdl/schematron">
    <let name="rootText" value="/*/PathNode/TextField/normalize-space(text())"/>
    <rule context="/*/SubNode">
        <assert id="R-F5V-T6B" role="ERROR" diagnostics="ND-SubNode_BT-00-Indicator" test="(../PathNode/IndicatorField) or (not(not($rootText = '')))">rule|text|R-F5V-T6B</assert>
    </rule>
    <rule context="/*">
        <assert id="R-W1D-J2Y" role="ERROR" diagnostics="ND-Root_BT-00-Indicator" test="PathNode/IndicatorField">rule|text|R-W1D-J2Y</assert>
    </rule>
</pattern>
