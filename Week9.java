public class Week9 {
    // Task 1: Access Control Proxy
    interface Image {
        void show();
    }

    static class RealImage implements Image {
        private String fileName;

        public RealImage(String fileName) {
            this.fileName = fileName;
            loadFromDisk();
        }

        private void loadFromDisk() {
            System.out.println("[SLOW] Reading " + fileName + " from hard drive...");
        }

        @Override
        public void show() {
            System.out.println("Displaying " + fileName + " on screen!");
        }
    }

    static class ProxyImage implements Image {
        private String fileName;
        private String userRole;
        private RealImage realImage;

        public ProxyImage(String fileName, String userRole) {
            this.fileName = fileName;
            this.userRole = userRole;
        }

        @Override
        public void show() {
            if (!"ADMIN".equals(userRole)) {
                System.out.println("[ACCESS DENIED] You lack permission to view: " + fileName);
                return;
            }
            if (realImage == null) {
                realImage = new RealImage(fileName);
            }
            realImage.show();
        }
    }

    // Task 2: Tracking & Logging Proxy
    interface Video {
        void play();
    }

    static class RealVideo implements Video {
        private String fileName;

        public RealVideo(String fileName) {
            this.fileName = fileName;
            download();
        }

        private void download() {
            System.out.println("--> [DOWNLOADING] Connecting to server & buffering 4K video: " + fileName);
        }

        @Override
        public void play() {
            System.out.println("Playing video: " + fileName);
        }
    }

    static class ProxyVideo implements Video {
        private String fileName;
        private RealVideo realVideo;
        private int viewCount = 0;

        public ProxyVideo(String fileName) {
            this.fileName = fileName;
        }

        @Override
        public void play() {
            viewCount++;
            System.out.println("--> [AUDIT] Video has been played " + viewCount + " time(s).");
            if (realVideo == null) {
                realVideo = new RealVideo(fileName);
            }
            realVideo.play();
        }
    }

    // Task 3: School Wi-Fi Firewall
    interface Internet {
        void connectTo(String site);
    }

    static class RealInternet implements Internet {
        @Override
        public void connectTo(String site) {
            System.out.println("Connected to site: " + site);
        }
    }

    static class SchoolProxyInternet implements Internet {
        private RealInternet realInternet;

        @Override
        public void connectTo(String site) {
            if ("gaming.com".equalsIgnoreCase(site) || "socialmedia.com".equalsIgnoreCase(site)) {
                System.out.println("[BLOCKED BY FIREWALL] Cannot access: " + site);
                return;
            }
            if (realInternet == null) {
                realInternet = new RealInternet();
            }
            realInternet.connectTo(site);
        }
    }

    // Task 4 & 5: Devices & Remotes
    interface Device {
        void turnon();
        void turnoff();
        void setVolume(int level);
    }

    static class SmartLight implements Device {
        @Override
        public void turnon() {
            System.out.println("Smart Light glows bright white.");
        }

        @Override
        public void turnoff() {
            System.out.println("Smart Light shuts off.");
        }

        @Override
        public void setVolume(int level) {
            System.out.println("Smart Light brightness set to: " + level + "%");
        }
    }

    static class Radio implements Device {
        @Override
        public void turnon() {
            System.out.println("Radio starts playing static/music.");
        }

        @Override
        public void turnoff() {
            System.out.println("Radio turned off.");
        }

        @Override
        public void setVolume(int level) {
            System.out.println("Radio volume knob turned to: " + level);
        }
    }

    static class BasicRemote {
        protected Device device;

        public BasicRemote(Device device) {
            this.device = device;
        }

        public void poweron() {
            device.turnon();
        }

        public void poweroff() {
            device.turnoff();
        }
    }

    static class AdvancedRemote extends BasicRemote {
        public AdvancedRemote(Device device) {
            super(device);
        }

        public void mute() {
            System.out.println("[Remote] Mute button pressed!");
            device.setVolume(0);
        }
    }

    static class PartyRemote extends BasicRemote {
        public PartyRemote(Device device) {
            super(device);
        }

        public void partyMode() {
            System.out.println("[Party Remote] Pumping up the party!");
            device.turnon();
            device.setVolume(100);
        }
    }

    // Task 6: RPG Game Weapon System
    interface Weapon {
        void use();
    }

    static class Sword implements Weapon {
        @Override
        public void use() {
            System.out.println("Slash! Dealing 20 physical damage.");
        }
    }

    static class MagicWand implements Weapon {
        @Override
        public void use() {
            System.out.println("Zap! Dealing 35 magic damage.");
        }
    }

    static abstract class Hero {
        protected Weapon weapon;

        public Hero(Weapon weapon) {
            this.weapon = weapon;
        }

        public abstract void attack();
    }

    static class Knight extends Hero {
        public Knight(Weapon weapon) {
            super(weapon);
        }

        @Override
        public void attack() {
            weapon.use();
        }
    }

    static class Wizard extends Hero {
        public Wizard(Weapon weapon) {
            super(weapon);
        }

        @Override
        public void attack() {
            weapon.use();
        }
    }

    public static void main(String[] args) {
        // --- Task 1 ---[span_0](start_span)[span_0](end_span)
        Image guestProxy = new ProxyImage("salary_sheet.png", "GUEST");
        guestProxy.show();
        Image adminProxy = new ProxyImage("salary_sheet.png", "ADMIN");
        adminProxy.show();

        // --- Task 2 ---[span_1](start_span)[span_1](end_span)
        Video videoProxy = new ProxyVideo("Cat_Memes.mp4");
        videoProxy.play();
        videoProxy.play();
        videoProxy.play();

        // --- Task 3 ---[span_2](start_span)[span_2](end_span)
        Internet internet = new SchoolProxyInternet();
        internet.connectTo("wikipedia.org");
        internet.connectTo("gaming.com");

        // --- Task 4 ---[span_3](start_span)[span_3](end_span)[span_4](start_span)[span_4](end_span)
        Device light = new SmartLight();
        AdvancedRemote remote = new AdvancedRemote(light);
        remote.poweron();
        remote.mute();

        // --- Task 5 ---[span_5](start_span)[span_5](end_span)
        Device radio = new Radio();
        PartyRemote partyRemote = new PartyRemote(radio);
        partyRemote.partyMode();

        // --- Task 6 ---[span_6](start_span)[span_6](end_span)
        Hero knightWithSword = new Knight(new Sword());
        knightWithSword.attack();

        Hero knightWithWand = new Knight(new MagicWand());
        knightWithWand.attack();
    }
}
