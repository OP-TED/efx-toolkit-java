<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-2" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/PathNode/TextField">
        <report id="R-X3F-N8W" role="INFO" test="(../NumberField/number() &gt; 0) or (not(./normalize-space(text()) = 'restricted'))">rule|text|R-X3F-N8W</report>
        <assert id="R-H9T-V5L" role="WARNING" test="(../IndicatorField) or (not(./normalize-space(text()) = 'negotiated'))">rule|text|R-H9T-V5L</assert>
        <report id="R-B6J-C4R" role="INFO" test="(./normalize-space(text()) != '') or (not(not(./normalize-space(text()) = 'open') and not(./normalize-space(text()) = 'restricted') and not(./normalize-space(text()) = 'negotiated')))">rule|text|R-B6J-C4R</report>
    </rule>
</pattern>
