<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <${Action} xmlns="http://ec2.amazonaws.com/doc/${Version}/">
         <volumeId>${VolumeId}</volumeId>
         <#if InstanceId??>
         <instanceId>${InstanceId}</instanceId>
         </#if>
         <#if Device??>
         <device>${Device}</device>
         </#if>
         <#if Force??>
         <force>${Force}</force>
         </#if>
      </${Action}>
   </soap:Body>
</soap:Envelope>