<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <${Action} xmlns="http://ec2.amazonaws.com/doc/${Version}/">
         <userId><#if UserId??>${UserId}</#if></userId>
         <groupName>${GroupName}</groupName>
         <ipPermissions>
            <item>
               <ipProtocol>${IpProtocol}</ipProtocol>
               <fromPort>${FromPort}</fromPort>
               <toPort>${ToPort}</toPort>
               <groups>
<#if SourceSecurityGroupOwnerId?? && SourceSecurityGroupName??>
                  <item>
                     <userId>${SourceSecurityGroupOwnerId}</userId>
                     <groupName>${SourceSecurityGroupName}</groupName>
                  </item>
</#if>
               </groups>
               <ipRanges>
<#if CidrIp??>
                  <item>
                     <cidrIp>${CidrIp}</cidrIp>
                  </item>
</#if>
               </ipRanges>
            </item>
         </ipPermissions>
      </${Action}>
   </soap:Body>
</soap:Envelope>