<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-4b-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/RepeatableNode/FieldsPrivacy">
        <let name="reason" value="ReasonCode/normalize-space(text())"/>
        <assert id="R-EVR-010" role="ERROR" diagnostics="ND-PrivacyInRepeatableNode_BT-197_BT-00_-Text-In-Repeatable-Node" test="($reason != '') or (not(exists(PublicationDate)))">rule|text|R-EVR-010</assert>
    </rule>
</pattern>
