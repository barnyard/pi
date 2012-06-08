<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <${Action} xmlns="http://ec2.amazonaws.com/doc/${Version}/">
<#if InstanceId??>
          <instancesSet>
	<#list InstanceId as item>   
		     <item>
			    <instanceId>${item}</instanceId>
		     </item>
	</#list>
          </instancesSet>
</#if>
      </${Action}>
   </soap:Body>
</soap:Envelope>
