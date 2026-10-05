package com.comspare.common;

import com.comspare.inventory.PartService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final PartService partService;

    public HomeController(PartService partService) {
        this.partService = partService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("totalParts", partService.getActiveParts().size());
        model.addAttribute("lowStockCount", partService.countLowStockParts());
        return "home";
    }
}
