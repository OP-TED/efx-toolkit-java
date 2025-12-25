<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-2" xmlns="http://purl.oclc.org/dsdl/schematron">
    <let name="stageVar1" value="&quot;S1&quot;"/>
    <let name="stageVar2" value="&quot;S2&quot;"/>
    <let name="before1" value="&quot;b1&quot;"/>
    <let name="preA" value="&quot;A&quot;"/>
    <let name="preB" value="&quot;B&quot;"/>
    <let name="left" value="&quot;L&quot;"/>
    <let name="p1" value="&quot;P1&quot;"/>
    <let name="p2" value="&quot;P2&quot;"/>
    <rule context="/*/PathNode/TextField">
        <assert id="R-X3F-N8W" role="error" test="./normalize-space(text()) != ''">rule|text|R-X3F-N8W</assert>
    </rule>
    <rule context="/*/PathNode/NumberField">
        <assert id="R-D4K-P9M" role="error" test="$before1 != &quot;x&quot;">rule|text|R-D4K-P9M</assert>
    </rule>
    <rule context="/*/PathNode/IndicatorField">
        <assert id="R-W1D-J2Y" role="error" test="$preA != $preB">rule|text|R-W1D-J2Y</assert>
    </rule>
    <rule context="/*/PathNode/TextField">
        <let name="after1" value="&quot;a1&quot;"/>
        <assert id="R-S9L-R5K" role="error" test="$after1 != &quot;x&quot;">rule|text|R-S9L-R5K</assert>
    </rule>
    <rule context="/*/PathNode/NumberField">
        <let name="postX" value="&quot;X&quot;"/>
        <let name="postY" value="&quot;Y&quot;"/>
        <assert id="R-A8Q-H3W" role="error" test="$postX != $postY">rule|text|R-A8Q-H3W</assert>
    </rule>
    <rule context="/*/PathNode/IndicatorField">
        <let name="right" value="&quot;R&quot;"/>
        <assert id="R-J5B-Y9S" role="error" test="$left != $right">rule|text|R-J5B-Y9S</assert>
    </rule>
    <rule context="/*/PathNode/TextField">
        <let name="r1" value="&quot;R1&quot;"/>
        <let name="r2" value="&quot;R2&quot;"/>
        <assert id="R-T6N-K8R" role="error" test="$p1 != $r1 and $p2 != $r2">rule|text|R-T6N-K8R</assert>
    </rule>
</pattern>
