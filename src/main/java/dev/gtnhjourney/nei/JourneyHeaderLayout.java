package dev.gtnhjourney.nei;

/** Pure header geometry. Keeps Journey controls away from NEI's native G/page-next utility cluster. */
final class JourneyHeaderLayout {

    static final int HEIGHT = 16;
    static final int GAP = 2;
    static final int SMALL = 16;
    static final int NEI_WIDTH = 24;

    private JourneyHeaderLayout() {}

    static Layout layout(int pagePrevX, int pagePrevY, int pagePrevW, int pageNextX, int pageNextW) {
        int x = pagePrevX + pagePrevW + GAP;
        Slot nei = slot(x, pagePrevY, NEI_WIDTH);
        x = nei.right() + GAP;
        Slot researched = slot(x, pagePrevY, SMALL);
        x = researched.right() + GAP;
        Slot favourite = slot(x, pagePrevY, SMALL);
        x = favourite.right() + GAP;
        Slot creative = slot(x, pagePrevY, SMALL);
        x = creative.right() + GAP;
        Slot delete = slot(x, pagePrevY, SMALL);
        int leftEnd = delete.right();

        // ItemPanel.resizeHeader places native G immediately left of pageNext with a 2 px gap.
        int nativeGX = pageNextX - pageNextW - GAP;
        Slot nativeG = slot(nativeGX, pagePrevY, pageNextW);

        RightCluster cluster = rightCluster(nativeG, pagePrevY, 2);
        if (!fits(leftEnd, cluster.latest)) cluster = rightCluster(nativeG, pagePrevY, 1);
        if (!fits(leftEnd, cluster.latest)) cluster = rightCluster(nativeG, pagePrevY, 0);

        return new Layout(
            nei,
            researched,
            favourite,
            creative,
            delete,
            cluster.latest,
            cluster.group,
            cluster.order,
            cluster.scan,
            cluster.debug,
            nativeG,
            cluster.serviceCount >= 1,
            cluster.serviceCount >= 2);
    }

    private static boolean fits(int leftEnd, Slot latest) {
        return latest != null && leftEnd + GAP <= latest.x;
    }

    private static RightCluster rightCluster(Slot nativeG, int y, int serviceCount) {
        int services = Math.max(0, Math.min(2, serviceCount));
        Slot immediateLeft = slot(nativeG.x - GAP - SMALL, y, SMALL);
        Slot debug = immediateLeft;
        Slot scan = slot(immediateLeft.x - GAP - SMALL, y, SMALL);
        int cursor;

        if (services >= 2) {
            cursor = scan.x;
        } else if (services == 1) {
            scan = immediateLeft;
            cursor = scan.x;
        } else {
            cursor = nativeG.x;
        }

        Slot order = slot(cursor - GAP - SMALL, y, SMALL);
        Slot group = slot(order.x - GAP - SMALL, y, SMALL);
        Slot latest = slot(group.x - GAP - SMALL, y, SMALL);
        return new RightCluster(latest, group, order, scan, debug, services);
    }

    private static Slot slot(int x, int y, int width) {
        return new Slot(x, y, width, HEIGHT);
    }

    private static final class RightCluster {
        final Slot latest;
        final Slot group;
        final Slot order;
        final Slot scan;
        final Slot debug;
        final int serviceCount;

        RightCluster(Slot latest, Slot group, Slot order, Slot scan, Slot debug, int serviceCount) {
            this.latest = latest;
            this.group = group;
            this.order = order;
            this.scan = scan;
            this.debug = debug;
            this.serviceCount = serviceCount;
        }
    }

    static final class Layout {
        final Slot nei;
        final Slot researched;
        final Slot favourite;
        final Slot creative;
        final Slot delete;
        final Slot latest;
        final Slot group;
        final Slot order;
        final Slot scan;
        final Slot debug;
        final Slot nativeG;
        final boolean scanVisible;
        final boolean debugVisible;

        Layout(
            Slot nei,
            Slot researched,
            Slot favourite,
            Slot creative,
            Slot delete,
            Slot latest,
            Slot group,
            Slot order,
            Slot scan,
            Slot debug,
            Slot nativeG,
            boolean scanVisible,
            boolean debugVisible) {
            this.nei = nei;
            this.researched = researched;
            this.favourite = favourite;
            this.creative = creative;
            this.delete = delete;
            this.latest = latest;
            this.group = group;
            this.order = order;
            this.scan = scan;
            this.debug = debug;
            this.nativeG = nativeG;
            this.scanVisible = scanVisible;
            this.debugVisible = debugVisible;
        }
    }

    static final class Slot {
        final int x;
        final int y;
        final int w;
        final int h;

        Slot(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        int right() {
            return x + w;
        }

        boolean contains(int px, int py) {
            return px >= x && py >= y && px < x + w && py < y + h;
        }

        boolean overlaps(Slot other) {
            return other != null && x < other.x + other.w && x + w > other.x && y < other.y + other.h && y + h > other.y;
        }
    }
}
