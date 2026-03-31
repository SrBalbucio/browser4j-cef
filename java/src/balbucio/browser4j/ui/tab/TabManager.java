package balbucio.browser4j.ui.tab;

import balbucio.browser4j.browser.api.Browser;
import balbucio.browser4j.browser.api.BrowserOptions;
import balbucio.browser4j.browser.api.CefBrowserImpl;
import balbucio.browser4j.core.runtime.BrowserRuntime;

import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

public class TabManager {
    private final Map<String, Tab> tabs;
    private final List<TabManagerListener> listeners;
    private final Container uiContainer;
    private String activeTabId;

    public TabManager() {
        this(null);
    }

    public TabManager(Container uiContainer) {
        this.tabs = new ConcurrentHashMap<>();
        this.listeners = new ArrayList<>();
        this.uiContainer = uiContainer;
        
        if (this.uiContainer != null && this.uiContainer.getLayout() == null) {
            this.uiContainer.setLayout(new BorderLayout());
        }
    }

    public void addListener(TabManagerListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(TabManagerListener listener) {
        this.listeners.remove(listener);
    }

    public Tab createTab(BrowserOptions options) {
        String id = UUID.randomUUID().toString();
        
        Browser browser = (options != null) 
            ? CefBrowserImpl.create(BrowserRuntime.getCefApp(), options)
            : CefBrowserImpl.create(BrowserRuntime.getCefApp());
            
        Tab tab = new Tab(id, browser);
        
        if (options != null && options.getSession() != null) {
            tab.getState().setIncognito(options.getSession().isIncognito());
        }

        tabs.put(id, tab);

        for (TabManagerListener listener : listeners) {
            listener.onTabCreated(tab);
        }

        if (activeTabId == null) {
            switchTo(id);
        }

        return tab;
    }

    public void switchTo(String tabId) {
        Tab targetTab = tabs.get(tabId);
        if (targetTab == null) return;

        targetTab.getState().markAccessed();
        this.activeTabId = tabId;

        for (TabManagerListener listener : listeners) {
            listener.onTabSwitched(targetTab);
        }

        if (uiContainer != null) {
            SwingUtilities.invokeLater(() -> {
                uiContainer.removeAll();
                Component ui = targetTab.getUIComponent();
                if (ui != null) {
                    uiContainer.add(ui, BorderLayout.CENTER);
                }
                uiContainer.revalidate();
                uiContainer.repaint();
            });
        }
    }

    public Tab getActiveTab() {
        if (activeTabId == null) return null;
        return tabs.get(activeTabId);
    }

    public Component getActiveComponent() {
        Tab active = getActiveTab();
        return (active != null) ? active.getUIComponent() : null;
    }

    public void closeTab(String tabId) {
        Tab tabToClose = tabs.remove(tabId);
        if (tabToClose != null) {
            // Memory clean up
            tabToClose.close();
            
            for (TabManagerListener listener : listeners) {
                listener.onTabClosed(tabId);
            }

            if (tabId.equals(activeTabId)) {
                this.activeTabId = null;
                // Fallback to the most recently accessed tab
                Tab nextTab = tabs.values().stream()
                        .max((t1, t2) -> Long.compare(t1.getState().getLastAccessedAt(), t2.getState().getLastAccessedAt()))
                        .orElse(null);

                if (nextTab != null) {
                    switchTo(nextTab.getId());
                } else if (uiContainer != null) {
                    SwingUtilities.invokeLater(() -> {
                        uiContainer.removeAll();
                        uiContainer.revalidate();
                        uiContainer.repaint();
                    });
                }
            }
        }
    }

    public void closeAll() {
        for (String id : new ArrayList<>(tabs.keySet())) {
            closeTab(id);
        }
    }

    public List<Tab> getIncognitoTabs() {
        List<Tab> incognitoTabs = new ArrayList<>();
        for (Tab tab : tabs.values()) {
            if (tab.getState().isIncognito()) {
                incognitoTabs.add(tab);
            }
        }
        return incognitoTabs;
    }

    public void closeAllIncognitoTabs() {
        for (Tab tab : getIncognitoTabs()) {
            closeTab(tab.getId());
        }
    }
}
