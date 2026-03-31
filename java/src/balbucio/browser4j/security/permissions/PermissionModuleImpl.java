package balbucio.browser4j.security.permissions;

import balbucio.browser4j.browser.api.Browser;
import java.nio.file.Path;

/**
 * Default implementation of the PermissionModule.
 */
public class PermissionModuleImpl implements PermissionModule {
    private final PermissionStore store;
    private PermissionAdapter adapter;

    public PermissionModuleImpl(Path profileDir) {
        this.store = new PermissionStore(profileDir);
    }

    @Override
    public void setPermission(String origin, PermissionType type, PermissionStatus status) {
        store.set(origin, type, status);
    }

    @Override
    public PermissionStatus getPermission(String origin, PermissionType type) {
        return store.get(origin, type);
    }

    @Override
    public boolean checkPermission(String origin, PermissionType type) {
        return store.get(origin, type) == PermissionStatus.GRANTED;
    }

    @Override
    public void setAdapter(PermissionAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public PermissionAdapter getAdapter() {
        return adapter;
    }
}
