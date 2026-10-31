package com.socialpath.service.impl;

import com.socialpath.dto.response.SessionView;
import com.socialpath.entity.UserSession;
import com.socialpath.repository.UserSessionRepository;
import com.socialpath.service.SessionService;
import com.socialpath.session.IpLocator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class SessionServiceImpl implements SessionService {

    /**
     * How often a used session is written back. Every request would mean a
     * database write per page, script and picture; this keeps it to one per
     * few minutes while still extending the session long before it expires.
     */
    static final Duration REFRESH_INTERVAL = Duration.ofMinutes(5);

    private final UserSessionRepository sessionRepository;
    private final IpLocator ipLocator;
    private final Duration timeout;

    public SessionServiceImpl(UserSessionRepository sessionRepository, IpLocator ipLocator,
                              @Value("${app.session.timeout}") Duration timeout) {
        this.sessionRepository = sessionRepository;
        this.ipLocator = ipLocator;
        this.timeout = timeout;
    }

    @Override
    public UserSession open(String login, String ip, String userAgent) {
        LocalDateTime now = LocalDateTime.now();
        return sessionRepository.save(new UserSession(login, ip, userAgent, ipLocator.locate(ip),
                now, now.plus(timeout)));
    }

    @Override
    public Optional<UserSession> findActive(String id, String login) {
        if (id == null) {
            return Optional.empty();
        }
        return sessionRepository.findById(id)
                .filter(session -> session.getUserLogin().equals(login))
                .filter(session -> session.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Override
    public boolean touch(UserSession session, String ip) {
        LocalDateTime now = LocalDateTime.now();
        if (session.getLastSeenAt().isAfter(now.minus(REFRESH_INTERVAL))) {
            return false;
        }
        session.setLastSeenAt(now);
        session.setExpiresAt(now.plus(timeout));
        if (ip != null && !ip.equals(session.getIp())) {
            session.setIp(ip);
            session.setLocation(ipLocator.locate(ip));
        }
        sessionRepository.save(session);
        return true;
    }

    @Override
    public List<SessionView> list(String login, String currentId) {
        // The current session first, the rest by recent use (the order the query returns).
        return sessionRepository.findByUserLoginOrderByLastSeenAtDesc(login).stream()
                .map(session -> SessionView.from(session, currentId))
                .sorted(Comparator.comparing((SessionView session) -> !session.current()))
                .toList();
    }

    @Override
    public void end(String id) {
        if (id != null) {
            sessionRepository.deleteById(id);
        }
    }

    @Override
    public void end(String login, String id) {
        sessionRepository.deleteByIdAndUserLogin(id, login);
    }

    @Override
    public void endOthers(String login, String keepId) {
        sessionRepository.deleteOthers(login, keepId);
    }

    @Override
    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT1M")
    public void removeExpired() {
        sessionRepository.deleteExpired(LocalDateTime.now());
    }
}
