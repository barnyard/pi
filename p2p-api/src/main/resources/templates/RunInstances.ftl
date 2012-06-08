<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
   <soap:Body>
      <${Action} xmlns="http://ec2.amazonaws.com/doc/${Version}/">
         <imageId>${ImageId}</imageId>
         <minCount>${MinCount}</minCount>
         <maxCount>${MaxCount}</maxCount>
<#if KeyName??>
         <keyName>${KeyName}</keyName>
</#if>
<#if SecurityGroup??>
         <groupSet>
   <#list SecurityGroup as item>   
            <item>
               <groupId>${item}</groupId>
            </item>
   </#list>
         </groupSet>
</#if>
<#if Placement??>
         <placement>
            <availabilityZone>${Placement.AvailabilityZone}</availabilityZone>
         </placement>
</#if>
<#if KernelId??>
         <kernelId>${KernelId}</kernelId>
</#if>
<#if RamdiskId??>
         <ramdiskId>${RamdiskId}</ramdiskId>
</#if>
<#if BlockDeviceMapping??>
         <blockDeviceMapping>
   <#list BlockDeviceMapping as item>   
            <item>
               <#if item.Virtual??>
               <virtualName>${item.Virtual}</virtualName>
               </#if>
               <#if item.Device??>
               <deviceName>${item.Device}</deviceName>
               </#if>
            </item>
   </#list>
         </blockDeviceMapping>
</#if>
<#if UserData??>
         <userData version="1.0" encoding="base64">
             <data>${UserData}</data>
         </userData>
</#if>
<#if AddressingType??>
         <addressingType>${AddressingType}</addressingType>
</#if>
<#if AdditionalInfo??>
	<additionalInfo>${AdditionalInfo}</additionalInfo>
</#if>
<#if InstanceType??>
	<instanceType>${InstanceType}</instanceType>
</#if>
<#if 20090404 <= VersionInteger>
   <#if Monitoring??>
         <monitoring><enabled>${Monitoring.Enabled}</enabled></monitoring>
   </#if>
</#if>
      </${Action}>
   </soap:Body>
</soap:Envelope>
