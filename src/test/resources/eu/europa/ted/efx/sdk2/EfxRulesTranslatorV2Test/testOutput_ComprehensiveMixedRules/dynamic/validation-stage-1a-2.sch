<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-2" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/PathNode/TextField">
        <assert id="R-K7P-M2Q" role="ERROR" test=".">rule|text|R-K7P-M2Q</assert>
        <report id="R-X3F-N8W" role="WARNING" test="./normalize-space(text()) = ''">rule|text|R-X3F-N8W</report>
        <assert id="R-H9T-V5L" role="ERROR" diagnostics="BT-00-Text_BT-00-Number" test="(not(./normalize-space(text()) = 'open')) or (../NumberField)">rule|text|R-H9T-V5L</assert>
        <report id="R-B6J-C4R" role="INFO" diagnostics="BT-00-Text_BT-00-Indicator" test="(not(./normalize-space(text()) = 'closed')) or (not(../IndicatorField))">rule|text|R-B6J-C4R</report>
    </rule>
    <rule context="/*/SubNode">
        <assert id="R-Y2N-G7S" role="ERROR" diagnostics="ND-SubNode_BT-00-Number" test="(not(../PathNode/TextField/normalize-space(text()) = 'pending')) or (../PathNode/NumberField)">rule|text|R-Y2N-G7S</assert>
        <assert id="R-D4K-P9M" role="WARNING" diagnostics="ND-SubNode_BT-00-Indicator" test="(not(../PathNode/TextField/normalize-space(text()) = 'active')) or (../PathNode/IndicatorField = true())">rule|text|R-D4K-P9M</assert>
        <report id="R-Z8H-A3X" role="INFO" diagnostics="ND-SubNode_BT-00-Text" test="(not(not(../PathNode/TextField/normalize-space(text()) = 'pending') and not(../PathNode/TextField/normalize-space(text()) = 'active'))) or (true())">rule|text|R-Z8H-A3X</report>
    </rule>
    <rule context="/*/PathNode/NumberField">
        <report id="R-F5V-T6B" role="INFO" diagnostics="BT-00-Number_BT-00-Text" test="(not(./number() &gt; 0)) or (../TextField/normalize-space(text()) != '')">rule|text|R-F5V-T6B</report>
        <report id="R-W1D-J2Y" role="WARNING" diagnostics="BT-00-Number_BT-00-Indicator" test="(not(./number() &lt; 100)) or (../IndicatorField)">rule|text|R-W1D-J2Y</report>
        <assert id="R-Q7G-E4Z" role="ERROR" test="(not(not(./number() &gt; 0) and not(./number() &lt; 100))) or (false())">rule|text|R-Q7G-E4Z</assert>
    </rule>
</pattern>
