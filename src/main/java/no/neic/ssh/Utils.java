package no.neic.ssh;

import com.jcraft.jsch.*;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import no.neic.tryggve.FolderNode;
import no.neic.tryggve.OneStepAuth;
import no.neic.tryggve.TwoStepsAuth;
import org.apache.commons.lang3.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Vector;

public final class Utils {
    private static Logger logger = LoggerFactory.getLogger(Utils.class);

    public static Session createSSHSession(String userName, String password, String hostName, int port, Optional<String> otc) throws JSchException {
        JSch jSch = new JSch();
        Session session = jSch.getSession(userName, hostName, port);
        session.setConfig("StrictHostKeyChecking", "no");
        if (otc.isPresent()) {
            session.setUserInfo(new TwoStepsAuth(password, otc.get()));
        } else {
            session.setUserInfo(new OneStepAuth(password));
        }
        session.connect();
        return session;
    }

    public static ChannelShell openSSHChannel(Session session) throws JSchException {
        ChannelShell channelShell = (ChannelShell) session.openChannel("shell");
        channelShell.setInputStream(new ByteArrayInputStream(new byte[32768]));
        channelShell.setOutputStream(new ByteArrayOutputStream(32768));
        channelShell.connect();
        return channelShell;
    }
}
