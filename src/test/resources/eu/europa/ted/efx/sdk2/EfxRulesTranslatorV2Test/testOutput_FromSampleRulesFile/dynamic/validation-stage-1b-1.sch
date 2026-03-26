<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1b-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*">
        <assert id="R-Y2N-G7S" role="ERROR" diagnostics="ND-Root_BT-00-Code" test="PathNode/CodeField">rule|text|R-Y2N-G7S</assert>
    </rule>
    <rule context="/*/SubNode">
        <assert id="R-D4K-P9M" role="ERROR" diagnostics="ND-SubNode_BT-00-Indicator" test="../PathNode/IndicatorField">rule|text|R-D4K-P9M</assert>
    </rule>
</pattern>
