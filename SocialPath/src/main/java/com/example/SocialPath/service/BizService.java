package com.example.SocialPath.service;

import com.example.SocialPath.document.Biz;
import com.example.SocialPath.extraClasses.BizCreationForm;
import jakarta.servlet.http.HttpServletRequest;

public interface BizService {
    Object[] validateBiz(BizCreationForm biz);
}
