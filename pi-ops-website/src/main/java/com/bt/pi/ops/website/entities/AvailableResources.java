package com.bt.pi.ops.website.entities;

import java.util.HashMap;
import java.util.Map;

public class AvailableResources {

	private Long freeMemoryInMB;

	private Long freeDiskInMB;

	private Integer freeCores;

	private Map<String, Long> availableInstancesByType;

	public AvailableResources() {
		availableInstancesByType = new HashMap<String, Long>();
	}

	public Long getFreeMemoryInMB() {
		return freeMemoryInMB;
	}

	public void setFreeMemoryInMB(Long theFreeMemoryInMB) {
		this.freeMemoryInMB = theFreeMemoryInMB;
	}

	public Long getFreeDiskInMB() {
		return freeDiskInMB;
	}

	public void setFreeDiskInMB(Long theFreeDiskInMB) {
		this.freeDiskInMB = theFreeDiskInMB;
	}

	public Integer getFreeCores() {
		return freeCores;
	}

	public void setFreeCores(Integer theFreeCores) {
		this.freeCores = theFreeCores;
	}

	public Map<String, Long> getAvailableInstancesByType() {
		return availableInstancesByType;
	}

	public void setAvailableInstancesByType(Map<String, Long> theAvailableInstancesByType) {
		this.availableInstancesByType = theAvailableInstancesByType;
	}

	public void addInstancesToType(String instanceType, long value) {
		if (availableInstancesByType.get(instanceType) == null) {
			availableInstancesByType.put(instanceType, value);
		} else {
			long currentInstances = availableInstancesByType.get(instanceType);
			availableInstancesByType.put(instanceType, currentInstances + value);
		}
	}

	@Override
	public String toString() {
		return "AvailableResources [freeMemoryInMB=" + freeMemoryInMB + ", freeDiskInMB=" + freeDiskInMB + ", freeCores=" + freeCores + ", availableInstancesByType=" + availableInstancesByType + "]";
	}
}
