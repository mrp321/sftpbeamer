package no.neic.ssh;


import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * This class is used to manage the ssh connection.
 */
public final class SSHSessionManager {
    private static Logger logger = LoggerFactory.getLogger(SSHSessionManager.class);
    private static final String HOST1 = "host1";
    private static final String HOST2 = "host2";

    private static SSHSessionManager instance = new SSHSessionManager();

    public static SSHSessionManager getManager() {
        return instance;
    }

    /**
     * The sftpConnectionHolderMap is used to keep the sftp connections of a user. The key is the session id.
     */
    private Map<String, SSHSessionHolder> sshConnectionHolderMap;

    private SSHSessionManager() {
        sshConnectionHolderMap = new HashMap<>();
    }

    public void createSSHSession(String sessionId, String source,
                                 String userName, String password, String otc, String hostName, int port) throws JSchException {
        if (otc.isEmpty()) {
            saveSSHSession(sessionId, source,
                    Utils.createSSHSession(userName, password, hostName, port, Optional.empty()), null);
        } else {
            saveSSHSession(sessionId, source,
                    Utils.createSSHSession(userName, password, hostName, port, Optional.of(otc)), null);
        }
    }

    public void createSSHSession(String sessionId, String source,
                                 String userName, String password, String hostName, int port) throws JSchException {
        saveSSHSession(sessionId, source,
                Utils.createSSHSession(userName, password, hostName, port, Optional.empty()),
                Utils.createSSHSession(userName, password, hostName, port, Optional.empty()));
    }

    private ChannelShell openSSHChannel(Session session) throws JSchException {
        return Utils.openSSHChannel(session);
    }

    public ChannelShell getSSHChannel(String sessionId, String source) throws JSchException {
        ChannelShell channelShell;
        if (source.equals(HOST1)) {
            channelShell = openSSHChannel(this.sshConnectionHolderMap.get(sessionId).getHost1());
        } else if (source.equals(HOST2)) {
            channelShell = openSSHChannel(this.sshConnectionHolderMap.get(sessionId).getHost2());
        } else {
            channelShell = null;
        }
        return channelShell;
    }

    public void disconnectSSH(String sessionId, String source) {

        if (sshConnectionHolderMap.containsKey(sessionId)) {
            if (source.equals(HOST1)) {
                if (sshConnectionHolderMap.get(sessionId).getHost1() != null) {
                    sshConnectionHolderMap.get(sessionId).getHost1().disconnect();
                    sshConnectionHolderMap.get(sessionId).setHost1(null);
                }
            }
            if (source.equals(HOST2)) {
                if (sshConnectionHolderMap.get(sessionId).getHost2() != null) {
                    sshConnectionHolderMap.get(sessionId).getHost2().disconnect();
                    sshConnectionHolderMap.get(sessionId).setHost2(null);
                }
            }
        }

    }

    public void disconnectSSH(String sessionId) {
        if (sshConnectionHolderMap.containsKey(sessionId)) {
            if (sshConnectionHolderMap.get(sessionId).getHost1() != null) {
                sshConnectionHolderMap.get(sessionId).getHost1().disconnect();
            }
            if (sshConnectionHolderMap.get(sessionId).getHost2() != null) {
                sshConnectionHolderMap.get(sessionId).getHost2().disconnect();
            }
            sshConnectionHolderMap.remove(sessionId);
        }

    }

    private void saveSSHSession(String sessionId, String source, Session session, Session sessionDownload) {
        if (sshConnectionHolderMap.containsKey(sessionId)) {
            if (source.equals(HOST1)) {
                sshConnectionHolderMap.get(sessionId).setHost1(session);
            }
            if (source.equals(HOST2)) {
                sshConnectionHolderMap.get(sessionId).setHost2(session);
            }
        } else {
            SSHSessionHolder holder = new SSHSessionHolder();
            if (source.equals(HOST1)) {
                holder.setHost1(session);
            }
            if (source.equals(HOST2)) {
                holder.setHost2(session);
            }
            sshConnectionHolderMap.put(sessionId, holder);
        }
    }


    @Data
    private class SSHSessionHolder {
        private Session host1;

        private Session host2;
    }

}
