<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <${Action} xmlns="http://ec2.amazonaws.com/doc/${Version}/">
         <volumeId>${VolumeId}</volumeId>
         <instanceId>${InstanceId}</instanceId>
         <device>${Device}</device>
      </${Action}>
   </soap:Body>
</soap:Envelope>