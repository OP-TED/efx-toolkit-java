<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*">
        <assert id="R-BHV-001" role="ERROR" diagnostics="ND-Root_BT-00-Text" test="exists(PathNode/TextField)">rule|text|R-BHV-001</assert>
        <assert id="R-BHV-002" role="ERROR" diagnostics="ND-Root_BT-00-Number" test="not(exists(PathNode/NumberField))">rule|text|R-BHV-002</assert>
        <assert id="R-BHV-003" role="ERROR" diagnostics="ND-Root_BT-00-Repeatable-Text" test="count(PathNode/RepeatableTextField/normalize-space(text())) &lt; 2">rule|text|R-BHV-003</assert>
        <assert id="R-BHV-004" role="ERROR" diagnostics="ND-Root_BT-00-Integer" test="PathNode/IntegerField/number() &gt;= 0 and PathNode/IntegerField/number() &lt;= 9999">rule|text|R-BHV-004</assert>
        <assert id="R-BHV-005" role="ERROR" diagnostics="ND-Root_BT-00-Number" test="PathNode/NumberField/number() &gt;= -1.5">rule|text|R-BHV-005</assert>
        <assert id="R-BHV-006" role="ERROR" diagnostics="ND-Root_BT-00-Number" test="fn:matches(normalize-space(PathNode/NumberField/text()), '^[0-9]+$')">rule|text|R-BHV-006</assert>
        <assert id="R-BHV-007" role="ERROR" diagnostics="ND-Root_BT-00-Text" test="fn:matches(normalize-space(PathNode/TextField/text()), '^O''Neil [0-9]{2}\.$')">rule|text|R-BHV-007</assert>
        <assert id="R-BHV-008" role="ERROR" diagnostics="ND-Root_BT-00-Code" test="PathNode/CodeField/normalize-space(text()) = ('code1','code2','code3')">rule|text|R-BHV-008</assert>
        <assert id="R-BHV-009" role="ERROR" diagnostics="ND-Root_BT-00-Indicator" test="string(PathNode/IndicatorField/(normalize-space(text()) = ('true', '1'))) = ('code1','code2','code3')">rule|text|R-BHV-009</assert>
        <assert id="R-BHV-010" role="ERROR" diagnostics="ND-Root_BT-00-Text" test="string-length(PathNode/TextField/normalize-space(text())) &lt;= 100">rule|text|R-BHV-010</assert>
        <assert id="R-BHV-011" role="ERROR" diagnostics="ND-Root_BT-00-Identifier" test="string-length(PathNode/IdField/normalize-space(text())) &gt;= 2 and string-length(PathNode/IdField/normalize-space(text())) &lt;= 100">rule|text|R-BHV-011</assert>
        <assert id="R-BHV-012" role="ERROR" diagnostics="ND-Root_BT-00-Repeatable-Text" test="count(PathNode/RepeatableTextField/normalize-space(text())) &gt;= 2">rule|text|R-BHV-012</assert>
        <assert id="R-BHV-013" role="ERROR" diagnostics="ND-Root_BT-00-Repeatable-Number" test="count(PathNode/RepeatableNumberField/number()) &gt;= 2 and count(PathNode/RepeatableNumberField/number()) &lt;= 5">rule|text|R-BHV-013</assert>
        <assert id="R-BHV-014" role="ERROR" diagnostics="ND-Root_BT-00-Amount-Currency" test="exists(PathNode/AmountField/@currencyID)">rule|text|R-BHV-014</assert>
        <assert id="R-BHV-015" role="ERROR" diagnostics="ND-Root_ND-RepeatableNode" test="count(RepeatableNode) &lt;= 3">rule|text|R-BHV-015</assert>
        <assert id="R-BHV-016" role="ERROR" diagnostics="ND-Root_ND-SubNode" test="not(exists(SubNode))">rule|text|R-BHV-016</assert>
    </rule>
    <rule context="/*/PathNode/IntegerField">
        <assert id="R-BHV-020" role="ERROR" test="./number() &gt;= 0 and ./number() &lt;= 9999">rule|text|R-BHV-020</assert>
        <assert id="R-BHV-021" role="ERROR" test="fn:matches(normalize-space(./text()), '^([0-9]+)$')">rule|text|R-BHV-021</assert>
    </rule>
    <rule context="/*/PathNode/TextMultilingualField">
        <assert id="R-BHV-022" role="ERROR" test="string-length(./normalize-space(text())) &lt;= 100">rule|text|R-BHV-022</assert>
    </rule>
    <rule context="/*/SubNode">
        <assert id="R-BHV-030" role="ERROR" diagnostics="ND-SubNode_BT-01-SubNode-Text" test="(exists(SubTextField)) or (not(../PathNode/IndicatorField/(normalize-space(text()) = ('true', '1')) = true()))">rule|text|R-BHV-030</assert>
        <assert id="R-BHV-031" role="ERROR" diagnostics="ND-SubNode_BT-01-SubNode-Text" test="(not(exists(SubTextField))) or (not(../PathNode/CodeField/normalize-space(text()) = 'x'))">rule|text|R-BHV-031</assert>
        <assert id="R-BHV-032" role="ERROR" diagnostics="ND-SubNode_BT-01-SubNode-Text" test="(string-length(SubTextField/normalize-space(text())) &lt;= 10) or (not(not(../PathNode/IndicatorField/(normalize-space(text()) = ('true', '1')) = true()) and not(../PathNode/CodeField/normalize-space(text()) = 'x')))">rule|text|R-BHV-032</assert>
    </rule>
    <rule context="/*/RepeatableNode">
        <assert id="R-BHV-040" role="ERROR" diagnostics="ND-RepeatableNode_BT-00-Text-In-Repeatable-Node" test="count(TextField/normalize-space(text())) &lt; 2">rule|text|R-BHV-040</assert>
    </rule>
</pattern>
