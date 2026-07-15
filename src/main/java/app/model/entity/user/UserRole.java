package app.model.entity.user;

import java.util.Set;

public enum UserRole {

    USER(Set.of()),

    PRODUCT_ADMIN(Set.of(
            Permission.PRODUCT_CREATE,
            Permission.PRODUCT_UPDATE,
            Permission.PRODUCT_DELETE
    )),

    ORDER_ADMIN(Set.of(
            Permission.ORDER_STATUS_UPDATE
    )),

    USER_ADMIN(Set.of(
            Permission.USER_VIEW,
            Permission.USER_ACTIVATE,
            Permission.USER_DEACTIVATE
    )),

    ADMIN(Set.of(Permission.values()));

    private final Set<Permission> permissions;

    UserRole(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }
}
