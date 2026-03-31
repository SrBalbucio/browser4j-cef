package balbucio.browser4j.ui.tab;

public interface TabManagerListener {
    void onTabCreated(Tab tab);
    void onTabClosed(String tabId);
    void onTabSwitched(Tab activeTab);
    void onDevToolsOpened(Tab tab);
    void onDevToolsClosed(Tab tab);
}
