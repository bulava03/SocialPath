package com.socialpath.dto.request;

import lombok.Data;

@Data
public class NewReport {
    private String authorLogin;
    private String subject;
    private String type;
    private String idPublication;
    private String idComment;
    private String idPage;
}
