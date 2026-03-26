<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <let name="stageVar1" value="&quot;S1&quot;"/>
    <let name="stageVar2" value="&quot;S2&quot;"/>
    <let name="before1" value="&quot;b1&quot;"/>
    <let name="preA" value="&quot;A&quot;"/>
    <let name="preB" value="&quot;B&quot;"/>
    <let name="left" value="&quot;L&quot;"/>
    <let name="p1" value="&quot;P1&quot;"/>
    <let name="p2" value="&quot;P2&quot;"/>
    <rule context="/*/PathNode/TextField">
        <assert id="R-K7P-M2Q" role="ERROR" test="not($stageVar1 = '')">rule|text|R-K7P-M2Q</assert>
    </rule>
    <rule context="/*/PathNode/NumberField">
        <assert id="R-Y2N-G7S" role="ERROR" test="not($before1 = '')">rule|text|R-Y2N-G7S</assert>
    </rule>
    <rule context="/*/PathNode/IndicatorField">
        <assert id="R-F5V-T6B" role="ERROR" test="not($preA = '') and not($preB = '')">rule|text|R-F5V-T6B</assert>
    </rule>
    <rule context="/*/PathNode/TextField">
        <let name="after1" value="&quot;a1&quot;"/>
        <assert id="R-M3C-U8N" role="ERROR" test="not($stageVar2 = '') and not($after1 = '')">rule|text|R-M3C-U8N</assert>
    </rule>
    <rule context="/*/PathNode/NumberField">
        <let name="postX" value="&quot;X&quot;"/>
        <let name="postY" value="&quot;Y&quot;"/>
        <assert id="R-V4T-O7J" role="ERROR" test="not($postX = '') and not($postY = '')">rule|text|R-V4T-O7J</assert>
    </rule>
    <rule context="/*/PathNode/IndicatorField">
        <let name="right" value="&quot;R&quot;"/>
        <assert id="R-G2M-X6D" role="ERROR" test="not($left = '') and not($right = '')">rule|text|R-G2M-X6D</assert>
    </rule>
    <rule context="/*/PathNode/TextField">
        <let name="r1" value="&quot;R1&quot;"/>
        <let name="r2" value="&quot;R2&quot;"/>
        <assert id="R-C1V-Z4H" role="ERROR" test="not($stageVar1 = '') and not($stageVar2 = '') and not($p1 = '')">rule|text|R-C1V-Z4H</assert>
    </rule>
</pattern>
