package com.socialpath.maintenance;

import com.socialpath.entity.Group;
import com.socialpath.entity.User;
import com.socialpath.repository.GroupRepository;
import com.socialpath.repository.UserRepository;
import com.socialpath.service.HandleAvatarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

/**
 * Converts, on startup, avatars uploaded before avatar processing existed:
 * each gets a square full version and a thumbnail under a new id. Only ids
 * referenced as user or group avatars are touched, never publication media
 * sharing the same directory.
 *
 * Disabled by default; enable it for a start with
 * app.avatars.convert-on-startup=true (the AVATAR_CONVERT_ON_STARTUP
 * environment variable in docker-compose). Running it again is harmless:
 * processed avatars have a thumbnail and are skipped.
 *
 * For each avatar the new files are written first, then the new id is saved,
 * and only then is the old file deleted, so a failure at any point never
 * leaves a reference to a missing file. A file that cannot be converted is
 * logged and left as it is.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.avatars.convert-on-startup", havingValue = "true")
@RequiredArgsConstructor
public class AvatarBackfillRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final HandleAvatarService handleAvatarService;

    @Override
    public void run(ApplicationArguments args) {
        int converted = 0;
        for (User user : userRepository.findByImageIdIsNotNull()) {
            if (convert("user " + user.getLogin(), user.getImageId(),
                    newId -> userRepository.updateImageId(user.getLogin(), newId))) {
                converted++;
            }
        }
        for (Group group : groupRepository.findAll()) {
            if (convert("group " + group.getId(), group.getImageId(),
                    newId -> groupRepository.updateImageId(group.getId(), newId))) {
                converted++;
            }
        }
        if (converted > 0) {
            log.info("Converted {} avatar(s) to the processed format", converted);
        }
    }

    private boolean convert(String owner, String imageId, Consumer<String> saveNewId) {
        if (!handleAvatarService.needsProcessing(imageId)) {
            return false;
        }
        try {
            String newId = handleAvatarService.reprocessStoredAvatar(imageId);
            saveNewId.accept(newId);
            handleAvatarService.deleteAvatar(imageId);
            log.info("Converted avatar of {}: {} -> {}", owner, imageId, newId);
            return true;
        } catch (Exception e) {
            log.warn("Could not convert avatar of {} ({}), leaving it as is: {}", owner, imageId, e.getMessage());
            return false;
        }
    }
}
