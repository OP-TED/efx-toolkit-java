<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-3a" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/SubNode/SubSubNode">
        <assert id="R-M3C-U8N" role="WARNING" diagnostics="ND-SubSubNode_BT-00-Indicator" test="(../../PathNode/IndicatorField) or (not(../../PathNode/TextField/normalize-space(text()) = &quot;expected&quot;))">rule|text|R-M3C-U8N</assert>
    </rule>
    <rule context="/*">
        <assert id="R-S9L-R5K" role="ERROR" diagnostics="ND-Root_BT-00-Code" test="PathNode/CodeField/normalize-space(text()) = (&quot;code1&quot;,&quot;code2&quot;,&quot;code3&quot;)">rule|text|R-S9L-R5K</assert>
    </rule>
</pattern>
