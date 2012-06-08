<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <${Action} xmlns="http://ec2.amazonaws.com/doc/${Version}/">
      	<volumeSet>
      	 <#if VolumeId??>
      	 <#list VolumeId as item>
      	 <item>
         	<volumeId>${item}</volumeId>
         </item>
         </#list>
         </#if>
         </volumeSet>
      </${Action}>
   </soap:Body>
</soap:Envelope>