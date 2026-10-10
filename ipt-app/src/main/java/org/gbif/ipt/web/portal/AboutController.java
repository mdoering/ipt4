package org.gbif.ipt.web.portal;

import org.gbif.ipt.web.support.page.GlobalModel;
import org.gbif.ipt.web.support.page.PageContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class AboutController {

  @GetMapping("/about")
  public String about(@ModelAttribute(name = GlobalModel.PAGE_CONTEXT, binding = false) PageContext ctx, Model model) {
    model.addAttribute(GlobalModel.PAGE, new AboutPage(ctx));
    return "about";
  }
}
