package rs.nopressurewear.constants;

/**
 * Stock-related defaults shared across the dashboard low-stock report, the PDF/Excel
 * exports and the low-stock e-mail alert job, so all three agree on what "low" means.
 */
public final class StockDefaults {

    private StockDefaults() {
    }

    /**
     * Default low-stock threshold: a variant with {@code stock_quantity <= } this value is "low".
     * String-typed so it can be referenced from {@code @RequestParam(defaultValue = ...)}.
     */
    public static final String LOW_STOCK_THRESHOLD = "5";

    /** Same value as an {@code int}, for use outside annotations. */
    public static final int LOW_STOCK_THRESHOLD_VALUE = Integer.parseInt(LOW_STOCK_THRESHOLD);
}
