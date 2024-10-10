package cn.edu.hit.filter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FilterManager {

    private final Set<String> blockedSites; // 禁止访问的网站
    private final Set<String> blockedUsers; // 禁止访问的用户
    private final Map<String, String> redirectMap; // 网站重定向（钓鱼）

    public FilterManager() {
        blockedSites = new HashSet<>();
        blockedUsers = new HashSet<>();
        redirectMap = new HashMap<>();
    }

    public FilterManager(Set<String> blockedSites, Set<String> blockedUsers, Map<String, String> redirectMap) {
        this.blockedSites = new HashSet<>(blockedSites);
        this.blockedUsers = new HashSet<>(blockedUsers);
        this.redirectMap = new HashMap<>(redirectMap);
    }

    public void addBlockedSite(String site) {
        blockedSites.add(site);
    }

    public boolean isSiteBlocked(String site) {
        return blockedSites.contains(site);
    }

    public void addBlockedUser(String user) {
        blockedUsers.add(user);
    }

    public boolean isUserBlocked(String user) {
        return blockedUsers.contains(user);
    }

    public void putRedirect(String targetSite, String redirectSite) {
        redirectMap.put(targetSite, redirectSite);
    }

    public String getRedirect(String site) {
        return redirectMap.getOrDefault(site, null);
    }
}
