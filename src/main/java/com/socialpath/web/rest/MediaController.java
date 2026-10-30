package com.socialpath.web.rest;

import com.socialpath.entity.VideoJob;
import com.socialpath.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {

    /** More placeholders than this are never on one page. */
    private static final int MAX_IDS = 50;

    private final VideoService videoService;

    /**
     * The current state of videos the feed shows as "processing", so the page
     * can swap a placeholder for the player once the conversion is done.
     * @param ids file ids of the videos
     * @return READY, PROCESSING or FAILED by file id
     */
    @GetMapping("/videoStatus")
    public Map<String, String> videoStatus(@RequestParam List<String> ids) {
        if (ids.size() > MAX_IDS) {
            throw new IllegalArgumentException("Too many videos asked at once");
        }
        Map<String, VideoJob.Status> jobs = videoService.statusOf(ids);
        Map<String, String> statuses = new LinkedHashMap<>();
        ids.forEach(id -> statuses.put(id, VideoService.displayStatus(jobs.get(id))));
        return statuses;
    }
}
