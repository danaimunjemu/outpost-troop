package zw.co.afc.orbit.outpost.troop.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import zw.co.afc.orbit.outpost.troop.dto.response.DiskMetrics;
import zw.co.afc.orbit.outpost.troop.service.iface.ServerService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/server")
public class ServerController {

    private final ServerService serverService;

    @GetMapping
    public DiskMetrics getCpuMetrics() {
        return  serverService.displaySpaceMetrics();
    }

}
