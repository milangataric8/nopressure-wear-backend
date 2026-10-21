package rs.nopressurewear.storefront.api;

import java.util.Optional;

/** Read-only settings access; callers never receive a managed settings entity. */
public interface SettingsQuery {
    Optional<String> findValue(String key);
}
