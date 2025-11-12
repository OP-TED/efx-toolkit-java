<#-- Template for individual Schematron pattern file -->
<pattern id="${id}" xmlns="http://purl.oclc.org/dsdl/schematron">
<#list variables as variable>
    <let name="${variable.name}" value="${variable.value}"/>
</#list>
<#list rules as rule>
    <rule context="${rule.context}">
    <#list rule.variables as variable>
        <let name="${variable.name}" value="${variable.value}"/>
    </#list>
    <#list rule.assertions as assertion>
        <assert id="${assertion.id}" role="${assertion.role}" test="${assertion.test}">${assertion.message}</assert>
    </#list>
    </rule>
</#list>
</pattern>
