<#-- Template for Schematron <rule> element -->
<rule context="${context}">
<#list variables as variable>
    <let name="${variable.name}" value="${variable.value}"/>
</#list>
<#list assertions as assertion>
    <assert id="${assertion.id}" role="${assertion.role}" test="${assertion.test}">${assertion.message}</assert>
</#list>
</rule>
