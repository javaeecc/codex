package cc.javaee.gateway.controller;

import cc.javaee.gateway.client.InviteSiteInfoClient;
import cc.javaee.gateway.config.SitePageProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

/**
 * 首页、登录页和注册页控制器。
 */
@Controller
public class PublicPageController {

    private final SitePageProperties sitePageProperties;
    private final InviteSiteInfoClient inviteSiteInfoClient;

    public PublicPageController(SitePageProperties sitePageProperties,
                                InviteSiteInfoClient inviteSiteInfoClient) {
        this.sitePageProperties = sitePageProperties;
        this.inviteSiteInfoClient = inviteSiteInfoClient;
    }

    @GetMapping("/")
    public Mono<String> home(Model model) {
        return render("public/home", model);
    }

    @GetMapping("/initialize")
    public String initialize() {
        return "public/initialize";
    }

    @GetMapping("/login")
    public Mono<String> login(@RequestParam(value = "redirect", required = false) String redirect,
                              Model model) {
        return render("public/login", model)
                .map(view -> {
                    model.addAttribute("redirectPath", redirect == null ? "/dashboard" : redirect);
                    return view;
                });
    }

    @GetMapping("/register")
    public Mono<String> register(Model model) {
        return render("public/register", model);
    }

    private Mono<String> render(String viewName, Model model) {
        return inviteSiteInfoClient.load(sitePageProperties.getInviteCode())
                .map(siteInfo -> {
                    model.addAttribute("siteName", siteInfo.getWebsiteTitle());
                    model.addAttribute("metaKeywords", siteInfo.getWebsiteKeywords());
                    model.addAttribute("metaDescription", siteInfo.getWebsiteDescription());
                    model.addAttribute("websiteImageBase64", siteInfo.getWebsiteImageBase64());
                    return viewName;
                });
    }
}
