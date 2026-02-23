<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*">
        <assert id="R-K7P-M2Q" role="ERROR" diagnostics="ND-Root_BT-00-Text" test="PathNode/TextField">rule|text|R-K7P-M2Q</assert>
    </rule>
    <rule context="/*/SubNode">
        <assert id="R-X3F-N8W" role="ERROR" diagnostics="ND-SubNode_BT-00-Text" test="(../PathNode/TextField) or (not(../PathNode/IndicatorField))">rule|text|R-X3F-N8W</assert>
    </rule>
    <rule context="/*/SubNode">
        <assert id="R-H9T-V5L" role="ERROR" diagnostics="ND-SubNode_BT-00-Text" test="(../PathNode/TextField) or (not(../PathNode/IndicatorField))">rule|text|R-H9T-V5L</assert>
        <assert id="R-B6J-C4R" role="WARNING" diagnostics="ND-SubNode_BT-00-Text" test="(not(../PathNode/TextField)) or (not(not(../PathNode/IndicatorField)))">rule|text|R-B6J-C4R</assert>
    </rule>
</pattern>
