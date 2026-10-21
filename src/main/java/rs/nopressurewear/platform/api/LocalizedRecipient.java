package rs.nopressurewear.platform.api;

/** Minimal locale view, independent of persistence and authentication. */
public interface LocalizedRecipient {
    String getPreferredLocale();
}
