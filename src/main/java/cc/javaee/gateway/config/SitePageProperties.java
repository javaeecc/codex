package cc.javaee.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * Site page properties loaded from the gateway working directory.
 */
@Component
@ConfigurationProperties(prefix = "codex.gateway.site")
public class SitePageProperties {

    private static final Logger log = LoggerFactory.getLogger(SitePageProperties.class);

    private String inviteCodeFile = "invite-code.txt";
    private String inviteCode;

    @PostConstruct
    public void loadInviteCode() {
        if (!StringUtils.hasText(refreshInviteCode())) {
            log.info("Gateway invite code file is not initialized: {}", inviteCodePath().toAbsolutePath());
        }
    }

    public synchronized String getInviteCode() {
        return refreshInviteCode();
    }

    public synchronized boolean isInitialized() {
        return StringUtils.hasText(refreshInviteCode());
    }

    private String refreshInviteCode() {
        Path path = inviteCodePath();
        if (!Files.isRegularFile(path)) {
            inviteCode = null;
            return null;
        }

        try {
            String value = new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
                    .replace("\uFEFF", "");
            inviteCode = normalize(value);
            return inviteCode;
        } catch (IOException ex) {
            log.error("Failed to read gateway invite code file: {}", path.toAbsolutePath(), ex);
            inviteCode = null;
            return null;
        }
    }

    public String getInviteCodeFile() {
        return inviteCodeFile;
    }

    public void setInviteCodeFile(String inviteCodeFile) {
        if (StringUtils.hasText(inviteCodeFile)) {
            this.inviteCodeFile = inviteCodeFile.trim();
        }
    }

    public synchronized boolean initializeInviteCode(String inviteCode) throws IOException {
        String normalizedInviteCode = normalize(inviteCode);
        if (!StringUtils.hasText(normalizedInviteCode)) {
            throw new IllegalArgumentException("Invite code must not be empty");
        }
        if (isInitialized()) {
            return false;
        }

        Path path = inviteCodePath();
        Path temporaryPath = path.resolveSibling(path.getFileName() + ".tmp");
        byte[] content = (normalizedInviteCode + System.lineSeparator()).getBytes(StandardCharsets.UTF_8);
        Files.write(temporaryPath, content, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        try {
            Files.move(temporaryPath, path, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(temporaryPath, path, StandardCopyOption.REPLACE_EXISTING);
        }
        this.inviteCode = normalizedInviteCode;
        log.info("Gateway invite code initialized at {}", path.toAbsolutePath());
        return true;
    }

    private Path inviteCodePath() {
        return Paths.get(System.getProperty("user.dir"), inviteCodeFile);
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
