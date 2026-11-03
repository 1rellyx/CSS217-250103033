import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class Week10 {
    public static void main(String[] args) throws Exception {
        System.out.println("==========================================");
        System.out.println("   TASK 1: STRATEGY + DECORATOR PATTERN   ");
        System.out.println("==========================================");
        runTask1();

        System.out.println("\n==========================================");
        System.out.println("    TASK 2: ADAPTER + PROXY PATTERN       ");
        System.out.println("==========================================");
        runTask2();

        System.out.println("\n==========================================");
        System.out.println("    TASK 3: BRIDGE + STRATEGY PATTERN     ");
        System.out.println("==========================================");
        runTask3();

        System.out.println("\n==========================================");
        System.out.println("   TASK 4: ADAPTER + DECORATOR PATTERN    ");
        System.out.println("==========================================");
        runTask4();

        System.out.println("\n==========================================");
        System.out.println("     TASK 5: BRIDGE + PROXY PATTERN       ");
        System.out.println("==========================================");
        runTask5();
    }

    // =========================================================================
    // TASK 1: STRATEGY + DECORATOR PATTERN
    // =========================================================================

    public interface ReportFormatter {
        String format(String title, String[] headers, String[][] rows);
    }

    public static class JsonFormatter implements ReportFormatter {
        @Override
        public String format(String title, String[] headers, String[][] rows) {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n  \"report\": \"").append(title).append("\",\n  \"records\": [\n");
            for (int i = 0; i < rows.length; i++) {
                sb.append("    {\n");
                for (int j = 0; j < headers.length; j++) {
                    sb.append("      \"").append(headers[j]).append("\": \"").append(rows[i][j]).append("\"");
                    if (j < headers.length - 1) sb.append(",");
                    sb.append("\n");
                }
                sb.append("    }").append(i < rows.length - 1 ? ",\n" : "\n");
            }
            sb.append("  ]\n}");
            return sb.toString();
        }
    }

    public static class CsvFormatter implements ReportFormatter {
        @Override
        public String format(String title, String[] headers, String[][] rows) {
            StringBuilder sb = new StringBuilder();
            sb.append("#TITLE: ").append(title).append("\n");
            sb.append(String.join(",", headers)).append("\n");
            for (String[] row : rows) {
                sb.append(String.join(",", row)).append("\n");
            }
            return sb.toString();
        }
    }

    public interface Exporter {
        void export(String destination, String data);
    }

    public static class BaseFileExporter implements Exporter {
        @Override
        public void export(String destination, String data) {
            System.out.printf("[DISK WRITE] Writing %d bytes to file://%s%n", data.length(), destination);
            System.out.println("--- FILE PAYLOAD BEGIN ---");
            System.out.println(data);
            System.out.println("--- FILE PAYLOAD END ---");
        }
    }

    public static abstract class ExporterDecorator implements Exporter {
        protected final Exporter wrappedExporter;

        public ExporterDecorator(Exporter wrappedExporter) {
            if (wrappedExporter == null) {
                throw new IllegalArgumentException("Wrapped exporter cannot be null");
            }
            this.wrappedExporter = wrappedExporter;
        }

        @Override
        public void export(String destination, String data) {
            wrappedExporter.export(destination, data);
        }
    }

    public static class HeaderFooterDecorator extends ExporterDecorator {
        private final String timestamp;

        public HeaderFooterDecorator(Exporter wrappedExporter, String timestamp) {
            super(wrappedExporter);
            this.timestamp = timestamp;
        }

        @Override
        public void export(String destination, String data) {
            String enrichedData = "=== METADATA HEADER: " + timestamp + " ===\n"
                    + data + "\n"
                    + "=== INTEGRITY CHECK: SHA-256 OK | END RECORD ===";
            super.export(destination, enrichedData);
        }
    }

    public static class Base64EncryptDecorator extends ExporterDecorator {
        public Base64EncryptDecorator(Exporter wrappedExporter) {
            super(wrappedExporter);
        }

        @Override
        public void export(String destination, String data) {
            String encoded = Base64.getEncoder().encodeToString(data.getBytes());
            String payload = "[BASE64_CIPHERTEXT:" + encoded + "]";
            super.export(destination, payload);
        }
    }

    private static void runTask1() {
        String[] headers = {"ID", "User", "Role"};
        String[][] rows = {
                {"101", "sufyan", "Admin"},
                {"102", "alex", "Dev"}
        };

        ReportFormatter jsonStrategy = new JsonFormatter();
        String rawJson = jsonStrategy.format("UserAuditReport", headers, rows);

        System.out.println(">>> TESTING DECORATOR STACK: Base64(HeaderFooter(BaseFileExporter)) <<<");
        Exporter securePipeline = new Base64EncryptDecorator(
                new HeaderFooterDecorator(new BaseFileExporter(), "2026-11-02 10:00:00 UTC")
        );
        securePipeline.export("/var/logs/audit.sec", rawJson);
    }

    // =========================================================================
    // TASK 2: ADAPTER + PROXY PATTERN
    // =========================================================================

    public static class LegacyStockFeed {
        public String fetchRawTickerPacket(String ticker) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException ignored) {}

            if ("AAPL".equalsIgnoreCase(ticker)) {
                return "TICKER=AAPL; BID=182.50; ASK=183.50; STATUS=OPEN; TIMESTAMP=17300000";
            } else if ("GOOGL".equalsIgnoreCase(ticker)) {
                return "TICKER=GOOGL; BID=164.20; ASK=164.80; STATUS=OPEN; TIMESTAMP=17300000";
            }
            return "TICKER=" + ticker + "; BID=0.00; ASK=0.00; STATUS=UNKNOWN; TIMESTAMP=0";
        }
    }

    public interface StockProvider {
        double getMarketPrice(String ticker);
        String getStatus(String ticker);
    }

    public static class StockFeedAdapter implements StockProvider {
        private final LegacyStockFeed legacyFeed;

        public StockFeedAdapter(LegacyStockFeed legacyFeed) {
            if (legacyFeed == null) throw new IllegalArgumentException("Legacy feed cannot be null");
            this.legacyFeed = legacyFeed;
        }

        private Map<String, String> parsePacket(String raw) {
            Map<String, String> map = new HashMap<>();
            String[] tokens = raw.split(";");
            for (String t : tokens) {
                String[] kv = t.trim().split("=");
                if (kv.length == 2) {
                    map.put(kv[0].trim(), kv[1].trim());
                }
            }
            return map;
        }

        @Override
        public double getMarketPrice(String ticker) {
            String raw = legacyFeed.fetchRawTickerPacket(ticker);
            Map<String, String> parsed = parsePacket(raw);
            double bid = Double.parseDouble(parsed.getOrDefault("BID", "0.0"));
            double ask = Double.parseDouble(parsed.getOrDefault("ASK", "0.0"));
            return (bid + ask) / 2.0;
        }

        @Override
        public String getStatus(String ticker) {
            String raw = legacyFeed.fetchRawTickerPacket(ticker);
            return parsePacket(raw).getOrDefault("STATUS", "UNAVAILABLE");
        }
    }

    public static class CachedStockProxy implements StockProvider {
        private static class CacheEntry {
            final double price;
            final long timestamp;

            CacheEntry(double price, long timestamp) {
                this.price = price;
                this.timestamp = timestamp;
            }
        }

        private final StockProvider realProvider;
        private final long ttlMillis;
        private final Map<String, CacheEntry> priceCache = new HashMap<>();

        public CachedStockProxy(StockProvider realProvider, long ttlMillis) {
            if (realProvider == null) throw new IllegalArgumentException("Real provider cannot be null");
            this.realProvider = realProvider;
            this.ttlMillis = ttlMillis;
        }

        @Override
        public double getMarketPrice(String ticker) {
            long now = System.currentTimeMillis();
            if (priceCache.containsKey(ticker)) {
                CacheEntry entry = priceCache.get(ticker);
                if ((now - entry.timestamp) < ttlMillis) {
                    System.out.printf("[CACHE HIT] Returning cached price for %s: $%.2f (Age: %d ms)%n",
                            ticker, entry.price, (now - entry.timestamp));
                    return entry.price;
                }
            }
            System.out.printf("[NETWORK FETCH] Cache miss or expired for %s. Calling upstream adapter...%n", ticker);
            double freshPrice = realProvider.getMarketPrice(ticker);
            priceCache.put(ticker, new CacheEntry(freshPrice, now));
            return freshPrice;
        }

        @Override
        public String getStatus(String ticker) {
            return realProvider.getStatus(ticker);
        }
    }

    private static void runTask2() throws InterruptedException {
        LegacyStockFeed legacyFeed = new LegacyStockFeed();
        StockProvider adapter = new StockFeedAdapter(legacyFeed);
        StockProvider proxy = new CachedStockProxy(adapter, 1500);

        System.out.println(">>> 1. Initial Request (Cold Cache) <<<");
        System.out.printf("AAPL Price: $%.2f%n", proxy.getMarketPrice("AAPL"));

        System.out.println("\n>>> 2. Immediate Repeat Request (Hot Cache) <<<");
        System.out.printf("AAPL Price: $%.2f%n", proxy.getMarketPrice("AAPL"));

        System.out.println("\n>>> 3. Sleeping 1600ms to invalidate cache... <<<");
        Thread.sleep(1600);

        System.out.println(">>> 4. Subsequent Request (Expired Cache) <<<");
        System.out.printf("AAPL Price: $%.2f%n", proxy.getMarketPrice("AAPL"));
    }

    // =========================================================================
    // TASK 3: BRIDGE + STRATEGY PATTERN
    // =========================================================================

    public interface DispatchStrategy {
        boolean shouldDispatch(int severityLevel, int currentHour);
        String getStrategyName();
    }

    public static class ImmediateStrategy implements DispatchStrategy {
        @Override
        public boolean shouldDispatch(int severityLevel, int currentHour) {
            return true;
        }

        @Override
        public String getStrategyName() {
            return "IMMEDIATE_DISPATCH";
        }
    }

    public static class BusinessHoursStrategy implements DispatchStrategy {
        @Override
        public boolean shouldDispatch(int severityLevel, int currentHour) {
            if (severityLevel >= 5) return true;
            return (currentHour >= 9 && currentHour <= 17);
        }

        @Override
        public String getStrategyName() {
            return "BUSINESS_HOURS_ONLY (09:00-17:00)";
        }
    }

    public interface MessageChannel {
        void transmit(String recipient, String message);
    }

    public static class EmailChannel implements MessageChannel {
        @Override
        public void transmit(String recipient, String message) {
            System.out.printf("[EMAIL PROTOCOL -> %s]%n%s%n", recipient, message);
        }
    }

    public static class SmsChannel implements MessageChannel {
        @Override
        public void transmit(String recipient, String message) {
            String condensed = message.replace("\n", " -- ");
            System.out.printf("[SMS PROTOCOL -> %s] %s%n", recipient, condensed);
        }
    }

    public static abstract class Alert {
        protected final MessageChannel channel;
        protected final DispatchStrategy strategy;

        public Alert(MessageChannel channel, DispatchStrategy strategy) {
            if (channel == null || strategy == null) throw new IllegalArgumentException("Dependencies cannot be null");
            this.channel = channel;
            this.strategy = strategy;
        }

        public void processAlert(String recipient, String rawContent, int currentHour) {
            int severity = getSeverityLevel();
            if (strategy.shouldDispatch(severity, currentHour)) {
                String formatted = formatAlertBody(rawContent);
                channel.transmit(recipient, formatted);
            } else {
                System.out.printf("[DISPATCH SUPPRESSED] Alert blocked by policy '%s' at hour %02d:00 (Severity: %d)%n",
                        strategy.getStrategyName(), currentHour, severity);
            }
        }

        public abstract int getSeverityLevel();
        public abstract String formatAlertBody(String raw);
    }

    public static class CriticalSystemAlert extends Alert {
        public CriticalSystemAlert(MessageChannel channel, DispatchStrategy strategy) {
            super(channel, strategy);
        }

        @Override
        public int getSeverityLevel() {
            return 5;
        }

        @Override
        public String formatAlertBody(String raw) {
            return "!!! CRITICAL PRODUCTION ALERT !!!\n"
                    + "Incident Report: " + raw + "\n"
                    + "ACTION REQUIRED: SRE on-call immediate intervention.";
        }
    }

    public static class DailySummaryAlert extends Alert {
        public DailySummaryAlert(MessageChannel channel, DispatchStrategy strategy) {
            super(channel, strategy);
        }

        @Override
        public int getSeverityLevel() {
            return 1;
        }

        @Override
        public String formatAlertBody(String raw) {
            return "Daily Digest:\n" + raw;
        }
    }

    private static void runTask3() {
        MessageChannel email = new EmailChannel();
        MessageChannel sms = new SmsChannel();

        DispatchStrategy businessHours = new BusinessHoursStrategy();

        System.out.println(">>> 1. Critical Alert via SMS (Night time: 02:00, Emergency Bypass) <<<");
        Alert emergency = new CriticalSystemAlert(sms, businessHours);
        emergency.processAlert("+1555019283", "Database primary node kernel panic", 2);

        System.out.println("\n>>> 2. Daily Summary via Email at Night (Hour: 23:00 - Should Suppress) <<<");
        Alert summaryNight = new DailySummaryAlert(email, businessHours);
        summaryNight.processAlert("dev-team@company.internal", "99.98% uptime achieved today.", 23);

        System.out.println("\n>>> 3. Daily Summary via Email during Workday (Hour: 11:00 - Should Deliver) <<<");
        Alert summaryDay = new DailySummaryAlert(email, businessHours);
        summaryDay.processAlert("dev-team@company.internal", "99.98% uptime achieved today.", 11);
    }

    // =========================================================================
    // TASK 4: ADAPTER + DECORATOR PATTERN
    // =========================================================================

    public static class LegacyForeignOrder {
        private final String orderId;
        private final double gbpAmount;

        public LegacyForeignOrder(String orderId, double gbpAmount) {
            this.orderId = orderId;
            this.gbpAmount = gbpAmount;
        }

        public String getOrderId() { return orderId; }
        public double getGbpAmount() { return gbpAmount; }
    }

    public static class LegacyCurrencyConverter {
        public double convert(double amount, String from, String to) {
            if ("GBP".equalsIgnoreCase(from) && "USD".equalsIgnoreCase(to)) {
                return amount * 1.25;
            }
            return amount;
        }
    }

    public interface PricingService {
        double calculateTotal();
        String getReceiptBreakdown();
    }

    public static class CurrencyOrderAdapter implements PricingService {
        private final LegacyForeignOrder foreignOrder;
        private final LegacyCurrencyConverter converter;
        private final String targetCurrency;

        public CurrencyOrderAdapter(LegacyForeignOrder order, LegacyCurrencyConverter converter, String targetCurrency) {
            this.foreignOrder = order;
            this.converter = converter;
            this.targetCurrency = targetCurrency;
        }

        @Override
        public double calculateTotal() {
            return converter.convert(foreignOrder.getGbpAmount(), "GBP", targetCurrency);
        }

        @Override
        public String getReceiptBreakdown() {
            return String.format("Order [%s] Base Item: $%.2f %s (Converted from %.2f GBP)",
                    foreignOrder.getOrderId(), calculateTotal(), targetCurrency, foreignOrder.getGbpAmount());
        }
    }

    public static abstract class PricingDecorator implements PricingService {
        protected final PricingService wrappedService;

        public PricingDecorator(PricingService wrappedService) {
            if (wrappedService == null) throw new IllegalArgumentException("Wrapped service cannot be null");
            this.wrappedService = wrappedService;
        }

        @Override
        public double calculateTotal() {
            return wrappedService.calculateTotal();
        }

        @Override
        public String getReceiptBreakdown() {
            return wrappedService.getReceiptBreakdown();
        }
    }

    public static class GiftWrapDecorator extends PricingDecorator {
        public GiftWrapDecorator(PricingService wrapped) {
            super(wrapped);
        }

        @Override
        public double calculateTotal() {
            return super.calculateTotal() + 5.00;
        }

        @Override
        public String getReceiptBreakdown() {
            return super.getReceiptBreakdown() + " + [Gift Wrap: $5.00]";
        }
    }

    public static class ExpressCourierDecorator extends PricingDecorator {
        public ExpressCourierDecorator(PricingService wrapped) {
            super(wrapped);
        }

        @Override
        public double calculateTotal() {
            return super.calculateTotal() + 15.00;
        }

        @Override
        public String getReceiptBreakdown() {
            return super.getReceiptBreakdown() + " + [Express Courier: $15.00]";
        }
    }

    public static class CustomsDutyDecorator extends PricingDecorator {
        public CustomsDutyDecorator(PricingService wrapped) {
            super(wrapped);
        }

        @Override
        public double calculateTotal() {
            double subtotal = super.calculateTotal();
            return Math.round((subtotal * 1.085) * 100.0) / 100.0;
        }

        @Override
        public String getReceiptBreakdown() {
            return super.getReceiptBreakdown() + " + [Customs Duty: 8.5%]";
        }
    }

    private static void runTask4() {
        LegacyForeignOrder order = new LegacyForeignOrder("ORD-9821", 100.00);
        LegacyCurrencyConverter converter = new LegacyCurrencyConverter();
        PricingService baseOrder = new CurrencyOrderAdapter(order, converter, "USD");

        System.out.printf("Base Converted: $%.2f%n", baseOrder.calculateTotal());

        PricingService completeCheckout = new CustomsDutyDecorator(
                new ExpressCourierDecorator(
                        new GiftWrapDecorator(baseOrder)
                )
        );

        System.out.println("\n>>> INVOICE BREAKDOWN <<<");
        System.out.println(completeCheckout.getReceiptBreakdown());
        System.out.printf("FINAL CHARGE TOTAL: $%.2f%n", completeCheckout.calculateTotal());
    }

    // =========================================================================
    // TASK 5: BRIDGE + PROXY PATTERN
    // =========================================================================

    public static class UserSession {
        private final String username;
        private final String role;

        public UserSession(String username, String role) {
            this.username = username;
            this.role = role;
        }

        public String getUsername() { return username; }
        public String getRole() { return role; }
    }

    public interface StorageDriver {
        byte[] loadRawBlob(String resourceUri);
        String getStorageType();
    }

    public static class S3StorageDriver implements StorageDriver {
        @Override
        public byte[] loadRawBlob(String resourceUri) {
            System.out.printf("[AWS S3 CLIENT] Performing HTTPS GET on bucket uri: s3://%s%n", resourceUri);
            return new byte[]{0x1F, (byte) 0x8B, 0x08, 0x00};
        }

        @Override
        public String getStorageType() {
            return "AMAZON_S3_CLOUD";
        }
    }

    public static class NfsStorageDriver implements StorageDriver {
        @Override
        public byte[] loadRawBlob(String resourceUri) {
            System.out.printf("[NFS POSIX CLIENT] Performing direct IO read on /mnt/nfs/%s%n", resourceUri);
            return new byte[]{0x4F, 0x67, 0x67, 0x53};
        }

        @Override
        public String getStorageType() {
            return "LOCAL_NFS_CLUSTER";
        }
    }

    public static class StorageDriverProxy implements StorageDriver {
        private final StorageDriver realDriver;
        private final UserSession session;

        public StorageDriverProxy(StorageDriver realDriver, UserSession session) {
            if (realDriver == null || session == null) throw new IllegalArgumentException("Dependencies cannot be null");
            this.realDriver = realDriver;
            this.session = session;
        }

        @Override
        public byte[] loadRawBlob(String resourceUri) {
            if ("GUEST".equalsIgnoreCase(session.getRole())) {
                System.out.printf("[PROXY BLOCKED] Access Denied for user '%s' [Role: %s] on resource: %s%n",
                        session.getUsername(), session.getRole(), resourceUri);
                return new byte[0];
            }
            System.out.printf("[PROXY AUDIT] Authorized user '%s' [Role: %s] accessing backend: %s%n",
                    session.getUsername(), session.getRole(), realDriver.getStorageType());
            return realDriver.loadRawBlob(resourceUri);
        }

        @Override
        public String getStorageType() {
            return "PROXY_GUARDED(" + realDriver.getStorageType() + ")";
        }
    }

    public static abstract class DigitalAsset {
        protected final StorageDriver driver;
        protected final String assetPath;

        public DigitalAsset(StorageDriver driver, String assetPath) {
            this.driver = driver;
            this.assetPath = assetPath;
        }

        public abstract void render();
    }

    public static class VideoAsset extends DigitalAsset {
        public VideoAsset(StorageDriver driver, String assetPath) {
            super(driver, assetPath);
        }

        @Override
        public void render() {
            System.out.printf("Initializing Video Player for: %s%n", assetPath);
            byte[] payload = driver.loadRawBlob(assetPath);
            if (payload.length > 0) {
                System.out.printf("SUCCESS: Video playback buffered %d bytes via %s%n", payload.length, driver.getStorageType());
            } else {
                System.out.println("FAILURE: Video buffer empty. Playback aborted.");
            }
        }
    }

    public static class AudioAsset extends DigitalAsset {
        public AudioAsset(StorageDriver driver, String assetPath) {
            super(driver, assetPath);
        }

        @Override
        public void render() {
            System.out.printf("Initializing Audio Stream for: %s%n", assetPath);
            byte[] payload = driver.loadRawBlob(assetPath);
            if (payload.length > 0) {
                System.out.printf("SUCCESS: Audio bitstream synchronized %d bytes via %s%n", payload.length, driver.getStorageType());
            } else {
                System.out.println("FAILURE: Audio stream inaccessible.");
            }
        }
    }

    private static void runTask5() {
        StorageDriver cloudS3 = new S3StorageDriver();
        StorageDriver localNfs = new NfsStorageDriver();

        UserSession adminUser = new UserSession("sufyan", "ADMIN");
        UserSession guestUser = new UserSession("anonymous", "GUEST");

        System.out.println(">>> 1. Authorized Video Playback via S3 (Admin Role) <<<");
        StorageDriver authorizedProxy = new StorageDriverProxy(cloudS3, adminUser);
        DigitalAsset video = new VideoAsset(authorizedProxy, "trailers/hyperion_4k.mp4");
        video.render();

        System.out.println("\n>>> 2. Unauthorized Audio Streaming via NFS (Guest Role - Blocked) <<<");
        StorageDriver blockedProxy = new StorageDriverProxy(localNfs, guestUser);
        DigitalAsset audio = new AudioAsset(blockedProxy, "master_audio/soundtrack.flac");
        audio.render();
    }
}
