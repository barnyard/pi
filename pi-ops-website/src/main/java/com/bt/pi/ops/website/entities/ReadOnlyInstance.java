package com.bt.pi.ops.website.entities;

import java.util.Date;

import com.bt.pi.app.common.entities.Instance;

public class ReadOnlyInstance {
	private Instance theInstance;

	public ReadOnlyInstance(Instance instance) {
		this.theInstance = instance;
	}

	public String getInstanceId() {
		return this.theInstance.getInstanceId();
	}

	public Date getInstanceActivityStateChangeTimestamp() {
		return new Date(theInstance.getInstanceActivityStateChangeTimestamp());
	}

	public String getInstanceActivityState() {
		return theInstance.getInstanceActivityState().toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((theInstance == null) ? 0 : theInstance.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReadOnlyInstance other = (ReadOnlyInstance) obj;
		if (theInstance == null) {
			if (other.theInstance != null)
				return false;
		} else if (!theInstance.equals(other.theInstance))
			return false;
		return true;
	}
}
