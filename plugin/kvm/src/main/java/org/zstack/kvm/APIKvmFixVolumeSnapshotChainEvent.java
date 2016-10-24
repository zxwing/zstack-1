package org.zstack.kvm;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIEvent;

import java.util.List;

/**
 * Created by xing5 on 2016/10/24.
 */
public class APIKvmFixVolumeSnapshotChainEvent extends APIEvent {
    public static class FixResult {
        private String volumeUuid;
        private String volumeName;
        private boolean success;
        private ErrorCode error;

        public String getVolumeUuid() {
            return volumeUuid;
        }

        public void setVolumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
        }

        public String getVolumeName() {
            return volumeName;
        }

        public void setVolumeName(String volumeName) {
            this.volumeName = volumeName;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public ErrorCode getError() {
            return error;
        }

        public void setError(ErrorCode error) {
            this.error = error;
        }
    }

    private List<FixResult> results;

    public List<FixResult> getResults() {
        return results;
    }

    public void setResults(List<FixResult> results) {
        this.results = results;
    }

    public APIKvmFixVolumeSnapshotChainEvent() {
    }

    public APIKvmFixVolumeSnapshotChainEvent(String apiId) {
        super(apiId);
    }
}
