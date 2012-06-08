# automatically generated config file for DHCP server
default-lease-time ${defaultLeaseTime};
max-lease-time ${maxLeaseTime};
ddns-update-style none;

shared-network pi {
<#list securityGroups as sg>

<#assign networkAddress = sg.networkAddress!"missing">
<#if networkAddress != "missing">
# security group ${sg.getOwnerIdGroupNamePair().getOwnerId()}/${sg.getOwnerIdGroupNamePair().getGroupName()}
subnet ${sg.networkAddress} netmask ${sg.netmask} {
	option subnet-mask ${sg.netmask};
	option broadcast-address ${sg.broadcastAddress};
	option domain-name-servers ${sg.dnsAddress};
	option routers ${sg.routerAddress};
}
<#assign instanceKeys = sg.instances?keys>
<#list instanceKeys as instance>
<#assign instanceAddress = sg.instances[instance]!"missing">
<#if instanceAddress != "missing">
<#assign privateAddress = instanceAddress.privateIpAddress!"missing">
<#assign macAddress = instanceAddress.macAddress!"missing">
<#if privateAddress != "missing" && macAddress != "missing">

# instance ${instance}
host node-${privateAddress} {
	hardware ethernet ${macAddress};
	fixed-address ${privateAddress};
	option interface-mtu ${mtu};
}
</#if>
</#if>
</#list>		
</#if>
</#list>
}