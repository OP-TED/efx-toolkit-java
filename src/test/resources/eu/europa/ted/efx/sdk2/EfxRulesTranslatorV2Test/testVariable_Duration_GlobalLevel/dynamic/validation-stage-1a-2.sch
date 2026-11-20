<?xml version="1.0" encoding="UTF-8"?>
<pattern id="EFORMS-validation-stage-1a-2" xmlns="http://purl.oclc.org/dsdl/schematron">
    <rule context="/*/PathNode/TextField">
        <assert id="R-VAR-006" role="ERROR" test="boolean(for $__T in (current-date()) return ($__T + $period &lt; $__T + xs:dayTimeDuration('P7D')))">rule|text|R-VAR-006</assert>
    </rule>
</pattern>
