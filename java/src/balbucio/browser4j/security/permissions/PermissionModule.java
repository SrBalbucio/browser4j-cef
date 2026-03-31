package balbucio.browser4j.security.permissions;

/**
 * High-level API for managing browser permissions.
 */
public interface PermissionModule {
    /**
     * Updates a saved permission decision.
     * @param origin the origin to save for
     * @param type the permission type
     * @param status the status to save
     */
    void setPermission(String origin, PermissionType type, PermissionStatus status);
    
    /**
     * Gets the current saved permission status for an origin.
     * Returns ASK if no decision is saved.
     */
    PermissionStatus getPermission(String origin, PermissionType type);
    
    /**
     * Manual check if a permission is granted.
     */
    boolean checkPermission(String origin, PermissionType type);
    
    /**
     * Sets an adapter to handle interactive permission requests.
     */
    void setAdapter(PermissionAdapter adapter);
    
    PermissionAdapter getAdapter();
}
