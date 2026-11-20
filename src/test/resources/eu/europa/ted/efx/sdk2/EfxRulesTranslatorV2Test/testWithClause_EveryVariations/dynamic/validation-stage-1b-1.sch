<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1b-1" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/RepeatableNode/FieldsPrivacy">
        <let name="priv" value="."/>
        <assert id="R-EVR-003" role="ERROR" diagnostics="ND-PrivacyInRepeatableNode_BT-197_BT-00_-Text-In-Repeatable-Node" test="exists(ReasonCode)">rule|text|R-EVR-003</assert>
    </rule>
</pattern>
