package zw.co.afc.orbit.outpost.troop.service.commandexec;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestBody;
import zw.co.afc.orbit.outpost.troop.dto.request.ServerDetails;

import java.io.IOException;

public interface CommandExec {

    String runLocalCommand(String command) throws IOException;
}
