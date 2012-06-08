<domain type='xen' id='${XEN_ID}'>
	<name>${NAME}</name>
	<os>
		<type>${OS_TYPE}</type>
		<#if OS_TYPE == "linux">
			<kernel>${BASEPATH}/${KERNEL_ID}</kernel>
			<#if use_ramdisk == "true">
				<initrd>${BASEPATH}/${RAMDISK_ID}</initrd>
			</#if>
			<#if TYPE == "linux">
				<root>/dev/sda1</root>
				<cmdline> ro</cmdline>
			</#if>
		</#if>
		<#if OS_TYPE == "hvm">
			<loader>/usr/lib/xen/boot/hvmloader</loader>
			<boot dev='hd'/>
		</#if>
		<#if TYPE == "opensolaris">
			<cmdline>/platform/i86xpv/kernel/amd64/unix -v -B zfs-bootfs=rpool/ROOT/opensolaris,bootpath=/xpvd/xdf\@51712:a</cmdline>
		</#if>
	</os>
	<memory>${MEMORY}</memory>
	<vcpu>${VCPUS}</vcpu>
	<on_crash>preserve</on_crash>
	<#if OS_TYPE == "hvm">
		<on_poweroff>destroy</on_poweroff>
		<on_reboot>restart</on_reboot>
		<on_crash>preserve</on_crash>
		<features>
			<acpi/>
			<apic/>
			<pae/>
		</features>
		<clock offset='utc'/>
	</#if>
	<devices>
		<#if OS_TYPE == "linux">
			<disk type='file'>
				<driver name='tap' type='aio' cache='default' /> 
				<source file='${BASEPATH}/${IMAGE_ID}'/>
				<#if TYPE == "linux">
					<target dev='sda1'/>
				</#if>
				<#if TYPE == "opensolaris">
		            <target dev='xvda'/>
				</#if>
			</disk>
			<#if use_ephemeral == "true">
				<disk type='file'>
					<driver name='tap' type='aio' cache='default' />
					<source file='${BASEPATH}/ephemeral'/>
					<#if TYPE == "linux">
						<target dev='sda2'/>
					</#if>
					<#if TYPE == "opensolaris">
			            <target dev='xvdb'/>
					</#if>
				</disk>
			</#if>
			<#if TYPE == "linux">
				<disk type='file'>
					<driver name='tap' type='aio' cache='default' />
					<source file='${SWAPPATH}/swap'/>
					<target dev='sda3'/>
				</disk>
			</#if>
		</#if>
		<#if OS_TYPE == "hvm">
			<emulator>/usr/lib64/xen/bin/qemu-dm</emulator>
		</#if>
		<interface type='bridge'>
			<source bridge='${BRIDGEDEV}'/>
			<mac address='${PRIVMACADDR}'/>
			<script path='/etc/xen/scripts/vif-bridge'/>
		</interface>
		<#if OS_TYPE == "hvm">
			<disk type='file' device='disk'>
				<driver name='tap' type='aio' cache='default' />
				<source file='${BASEPATH}/${IMAGE_ID}'/>
				<target dev='hda'/>
			</disk>
			<#if use_ephemeral == "true">
				<disk type='file'>
					<driver name='tap' type='aio' cache='default' />
					<source file='${BASEPATH}/ephemeral'/>
					<target dev='hdb'/>					
				</disk>
			</#if>
			<input type='tablet' bus='usb'/>
    		<input type='mouse' bus='ps2'/>
    		<graphics type='vnc' listen='0.0.0.0'/>
    		<console tty='/dev/pts/0'/>
		</#if>
	</devices>
</domain>
