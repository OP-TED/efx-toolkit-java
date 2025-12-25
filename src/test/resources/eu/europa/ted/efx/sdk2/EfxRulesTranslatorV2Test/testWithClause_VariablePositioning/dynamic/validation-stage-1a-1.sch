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
        <assert id="R-K7P-M2Q" role="error" test="$stageVar1 != ''">rule|text|R-K7P-M2Q</assert>
    </rule>
    <rule context="/*/PathNode/NumberField">
        <assert id="R-Y2N-G7S" role="error" test="$before1 != ''">rule|text|R-Y2N-G7S</assert>
    </rule>
    <rule context="/*/PathNode/IndicatorField">
        <assert id="R-F5V-T6B" role="error" test="$preA != '' and $preB != ''">rule|text|R-F5V-T6B</assert>
    </rule>
    <rule context="/*/PathNode/TextField">
        <let name="after1" value="&quot;a1&quot;"/>
        <assert id="R-M3C-U8N" role="error" test="$stageVar2 != '' and $after1 != ''">rule|text|R-M3C-U8N</assert>
    </rule>
    <rule context="/*/PathNode/NumberField">
        <let name="postX" value="&quot;X&quot;"/>
        <let name="postY" value="&quot;Y&quot;"/>
        <assert id="R-V4T-O7J" role="error" test="$postX != '' and $postY != ''">rule|text|R-V4T-O7J</assert>
    </rule>
    <rule context="/*/PathNode/IndicatorField">
        <let name="right" value="&quot;R&quot;"/>
        <assert id="R-G2M-X6D" role="error" test="$left != '' and $right != ''">rule|text|R-G2M-X6D</assert>
    </rule>
    <rule context="/*/PathNode/TextField">
        <let name="r1" value="&quot;R1&quot;"/>
        <let name="r2" value="&quot;R2&quot;"/>
        <assert id="R-C1V-Z4H" role="error" test="$stageVar1 != '' and $stageVar2 != '' and $p1 != ''">rule|text|R-C1V-Z4H</assert>
    </rule>
</pattern>
