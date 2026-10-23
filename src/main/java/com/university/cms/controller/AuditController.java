package com.university.cms.controller;

import com.university.cms.mapper.AuditMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/audit")
public class AuditController {

    private static final int PAGE_SIZE = 30;

    @Autowired
    private AuditMapper auditMapper;

    @GetMapping
    public String list(@RequestParam(defaultValue = "1") int page, Model model) {
        int total = auditMapper.count();
        int totalPages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        int current = Math.min(Math.max(1, page), totalPages);
        int offset = (current - 1) * PAGE_SIZE;

        model.addAttribute("list", auditMapper.findPage(offset, PAGE_SIZE));
        model.addAttribute("totalRows", total);
        model.addAttribute("page", current);
        model.addAttribute("totalPages", totalPages);
        return "admin/audit";
    }
}
