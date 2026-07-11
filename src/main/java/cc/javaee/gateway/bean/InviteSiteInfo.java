package cc.javaee.gateway.bean;

/**
 * 邀请码对应的网站信息 Bean。
 */
public class InviteSiteInfo {

    private final String websiteTitle;
    private final String websiteKeywords;
    private final String websiteDescription;
    private final String websiteImageBase64;

    public InviteSiteInfo(String websiteTitle,
                          String websiteKeywords,
                          String websiteDescription,
                          String websiteImageBase64) {
        this.websiteTitle = websiteTitle;
        this.websiteKeywords = websiteKeywords;
        this.websiteDescription = websiteDescription;
        this.websiteImageBase64 = websiteImageBase64;
    }

    public static InviteSiteInfo empty() {
        return new InviteSiteInfo(null, null, null, null);
    }

    public String getWebsiteTitle() {
        return websiteTitle;
    }

    public String getWebsiteKeywords() {
        return websiteKeywords;
    }

    public String getWebsiteDescription() {
        return websiteDescription;
    }

    public String getWebsiteImageBase64() {
        return websiteImageBase64;
    }
}
