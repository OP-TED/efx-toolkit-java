<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-2a-2" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/PathNode/IndicatorField">
        <report id="R-M3C-U8N" role="info" test=".">rule|text|R-M3C-U8N</report>
        <assert id="R-S9L-R5K" role="error" diagnostics="BT-00-Indicator_BT-00-Text" test="../TextField/normalize-space(text()) != ''">rule|text|R-S9L-R5K</assert>
        <assert id="R-N6P-I2F" role="warning" diagnostics="BT-00-Indicator_BT-00-Number" test="(not(. = true())) or (../NumberField/number() &gt; 0)">rule|text|R-N6P-I2F</assert>
    </rule>
    <rule context="/*">
        <report id="R-V4T-O7J" role="info" diagnostics="ND-Root_BT-00-Number" test="(not(PathNode/TextField)) or (PathNode/NumberField)">rule|text|R-V4T-O7J</report>
        <assert id="R-A8Q-H3W" role="error" diagnostics="ND-Root_BT-00-Indicator" test="(not(not(PathNode/TextField))) or (PathNode/IndicatorField)">rule|text|R-A8Q-H3W</assert>
    </rule>
</pattern>
