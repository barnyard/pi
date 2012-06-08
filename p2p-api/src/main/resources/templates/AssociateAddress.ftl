<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <${Action} xmlns="http://ec2.amazonaws.com/doc/${Version}/">
         <instanceId>${InstanceId}</instanceId>
         <publicIp>${PublicIp}</publicIp>
      </${Action}>
   </soap:Body>
</soap:Envelope>