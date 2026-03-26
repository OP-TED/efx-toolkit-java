<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-2a-2" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/PathNode/IndicatorField">
        <report id="R-M3C-U8N" role="INFO" test=".">rule|text|R-M3C-U8N</report>
        <assert id="R-S9L-R5K" role="ERROR" diagnostics="BT-00-Indicator_BT-00-Text" test="not(../TextField/normalize-space(text()) = '')">rule|text|R-S9L-R5K</assert>
        <assert id="R-N6P-I2F" role="WARNING" diagnostics="BT-00-Indicator_BT-00-Number" test="(../NumberField/number() &gt; 0) or (not(. = true()))">rule|text|R-N6P-I2F</assert>
    </rule>
    <rule context="/*">
        <report id="R-V4T-O7J" role="INFO" diagnostics="ND-Root_BT-00-Number" test="(PathNode/NumberField) or (not(PathNode/TextField))">rule|text|R-V4T-O7J</report>
        <assert id="R-A8Q-H3W" role="ERROR" diagnostics="ND-Root_BT-00-Indicator" test="(PathNode/IndicatorField) or (not(not(PathNode/TextField)))">rule|text|R-A8Q-H3W</assert>
    </rule>
</pattern>
