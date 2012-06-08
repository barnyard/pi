package com.bt.pi.ops.website.entities;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.pi.core.application.activation.ApplicationRecord;
import com.bt.pi.core.application.activation.TimeStampedPair;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.scope.NodeScope;

public class ApplicationRecordInfo {

	private static final Log LOG = LogFactory.getLog(ApplicationRecordInfo.class);
	private NodeScope nodeScope;
	private String value;
	private ApplicationRecord applicationRecord;

	public ApplicationRecordInfo() {
		super();

	}

	public ApplicationRecordInfo(NodeScope aNodeScope, String aValue, ApplicationRecord anApplicationRecord) {
		super();
		this.nodeScope = aNodeScope;
		this.value = aValue;
		this.applicationRecord = anApplicationRecord;
	}

	public NodeScope getNodeScope() {
		return nodeScope;
	}

	public String getValue() {
		return value;
	}

	public String getType() {
		return applicationRecord.getType();
	}

	public String getUriScheme() {
		return applicationRecord.getUriScheme();
	}

	public String getUrl() {
		return applicationRecord.getUrl();
	}

	public String getApplicationName() {
		return applicationRecord.getApplicationName();
	}

	public Map<String, TimeStampedPair<String>> getActiveNodeMap() {
		return applicationRecord.getActiveNodeMap();
	}

	public String toString() {
		return "Scope: " + nodeScope + ", value: " + value + "[" + super.toString() + "]";
	}

	public void removeActiveNode(String nodeId) {
		applicationRecord.removeActiveNode(nodeId);
	}

	public void updateApplicationRecord(BlockingDhtWriter writer, PId applicationId) {
		writer.update(applicationId, applicationRecord, new ApplicationRecordUpdateResolver());
	}

	private static final class ApplicationRecordUpdateResolver implements UpdateResolver<ApplicationRecord> {
		public ApplicationRecordUpdateResolver() {
		}

		@Override
		public ApplicationRecord update(ApplicationRecord existingEntity, ApplicationRecord requestedEntity) {
			if (existingEntity == null) {
				LOG.warn("Application Record for application " + requestedEntity.getApplicationName() + " was not found");
				return null;
			}
			return requestedEntity;
		}
	}
}