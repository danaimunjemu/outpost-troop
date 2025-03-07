package zw.co.afc.orbit.outpost.troop.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import oshi.SystemInfo;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import zw.co.afc.orbit.outpost.troop.dto.response.DiskInfo;
import zw.co.afc.orbit.outpost.troop.dto.response.DiskMetrics;
import zw.co.afc.orbit.outpost.troop.service.iface.ServerService;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ServerServiceImpl implements ServerService {

    @Override
    public DiskMetrics displaySpaceMetrics() {
        List<DiskInfo> diskList = new ArrayList<>();
        SystemInfo si = new SystemInfo();
        OperatingSystem os = si.getOperatingSystem();
        FileSystem fileSystem = os.getFileSystem();

        String mainDisk = null;
        long maxSpace = 0;

        for (OSFileStore fs : fileSystem.getFileStores()) {
            long total = fs.getTotalSpace();
            long free = fs.getUsableSpace();
            long used = total - free;
            double usagePercentage = (total > 0) ? ((double) used / total) * 100 : 0;
            String mount = fs.getMount();
            boolean isMain = false;

            // Identify the main disk
            if (mount.equals("/") || mount.equals("C:\\")) {
                mainDisk = fs.getName();
            } else if (total > maxSpace) {
                maxSpace = total;
                mainDisk = fs.getName();
            }

            diskList.add(new DiskInfo(
                    fs.getName(),
                    total / 1e9,
                    used / 1e9,
                    free / 1e9,
                    usagePercentage,
                    mount,
                    false
            ));
        }

        // Mark the main disk
        for (DiskInfo disk : diskList) {
            if (disk.name().equals(mainDisk)) {
                diskList.set(diskList.indexOf(disk),
                        new DiskInfo(disk.name(), disk.totalGB(), disk.usedGB(),
                                disk.freeGB(), disk.usagePercentage(), disk.mountPoint(), true)
                );
            }
        }

        return new DiskMetrics(diskList);

    }



}
