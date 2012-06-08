<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <${Action} xmlns="http://ec2.amazonaws.com/doc/${Version}/">
      	<securityGroupSet>
      	 <#if GroupName??>
      	 <#list GroupName as item>
      	 <item>
         	<groupName>${item}</groupName>
         </item>
         </#list>
         </#if>
         </securityGroupSet>
      </${Action}>
   </soap:Body>
</soap:Envelope>