<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <${Action} xmlns="http://ec2.amazonaws.com/doc/${Version}/">
         <availabilityZoneSet>
<#if ZoneName??>
   <#list ZoneName as item>
      	    <item>
         	   <zoneName>${item}</zoneName>
            </item>
   </#list>
</#if>
         </availabilityZoneSet>
      </${Action}>
   </soap:Body>
</soap:Envelope>