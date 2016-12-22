package org.zstack.storage.boss.backup;

import org.zstack.storage.boss.backup.BossBackupStorageBase.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by XXPS-PC1 on 2016/12/22.
 */
public class BossBackupStorageSimulatorConfig {
    public static class BossBackupStorageConfig {
        public long totalCapacity;
        public long availCapacity;
        public String clusterName;
        public String name;
    }

    public volatile boolean clusterInitSuccess = true;
    public List<InitCmd> initCmds = new ArrayList<InitCmd>();
    public Map<String, BossBackupStorageConfig> config = new HashMap<String, BossBackupStorageConfig>();
    public List<DownloadCmd> downloadCmds = new ArrayList<DownloadCmd>();
    public List<DeleteCmd> deleteCmds = new ArrayList<DeleteCmd>();
    public List<PingCmd> pingCmds = new ArrayList<PingCmd>();
    public Map<String, Long> imageSize = new HashMap<String, Long>();
    public Map<String, Long> imageActualSize = new HashMap<String, Long>();

    public List<GetImageSizeCmd> getImageSizeCmds = new ArrayList<GetImageSizeCmd>();
    public Map<String, Long> getImageSizeCmdSize = new HashMap<String, Long>();
    public Map<String, Long> getImageSizeCmdActualSize = new HashMap<String, Long>();

    public Map<String, Boolean> pingCmdSuccess = new HashMap<String, Boolean>();
    //public Map<String, PingOperationFailure> pingCmdOperationFailure = new HashMap<>();
    //public List<GetFactsCmd> getFactsCmds = new ArrayList<GetFactsCmd>();
    //public Map<String, String> getFactsCmdFsid = new HashMap<String, String>();
}
