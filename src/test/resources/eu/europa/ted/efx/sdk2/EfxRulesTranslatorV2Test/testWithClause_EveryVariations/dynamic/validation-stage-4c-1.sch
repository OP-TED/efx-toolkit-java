<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-4c-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/RepeatableNode/FieldsPrivacy[FieldIdentifierCode/text()='test-priv']">
        <let name="reason" value="ReasonCode/normalize-space(text())"/>
        <assert id="R-EVR-011" role="ERROR" diagnostics="ND-PrivacyInRepeatableNode_BT-197_BT-00_-Text-In-Repeatable-Node" test="($reason != '') or (not(exists(PublicationDate)))">rule|text|R-EVR-011</assert>
    </rule>
</pattern>
