package zw.co.afc.orbit.outpost.troop.dto.response;

public record DiskInfo(
        String name,
        double totalGB,
        double usedGB,
        double freeGB,
        double usagePercentage,
        String mountPoint,
        boolean isMainDisk
) {
}
