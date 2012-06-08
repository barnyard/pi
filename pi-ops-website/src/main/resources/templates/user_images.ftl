<html>
<head>
<title>
Images available to user ${username}
</title>
</head>
<body>
	<#assign row=0>
	<#list images as image>
		<#if (row % 2) == 0>
			<#assign bgcolor="lightblue">
		<#else>
			<#assign bgcolor="cream">
		</#if>
		<#assign row=row + 1>
		<table style="background-color: ${bgcolor};">
			<tr><td>Image Id:</td><td>${image.imageId}</td></tr>
			<tr><td colspan="2">This image is <#if image.isPublic()>public<#else>private</#if></td></tr>
			<tr><td>Owner:</td><td><a href="/users/${image.ownerId}">${image.ownerId}</a></td></tr>
			<#if (image.platform == "linux") >
				<tr><td>Platform:</td><td>${image.platform}</td></tr>
				<#if (image.machineType == "MACHINE")>
					<tr><td>Kernel:</td><td>${image.kernelId!'(none registered)'}</td></tr>
					<tr><td>Ramdisk:</td><td>${image.ramdiskId!'(none registered)'}</td></tr>
				<#else>
					<tr><td>Machine type:</td><td>${image.machineType}</td></tr>
				</#if>
			<#else>
				<tr><td>Platform:</td><td>${image.platform}</td></tr>
			</#if>
			<tr><td>Architecture:</td><td>${image.architecture!''}</td></tr>
			<tr><td>Manifest location:</td><td>${image.manifestLocation!''}</td></tr>
			<tr><td>State:</td><td>${image.state}</td></tr>
		</table>
	</#list>
</body>